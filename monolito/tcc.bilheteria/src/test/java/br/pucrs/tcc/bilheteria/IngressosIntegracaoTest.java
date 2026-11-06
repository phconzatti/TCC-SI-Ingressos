package br.pucrs.tcc.bilheteria;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import br.pucrs.tcc.bilheteria.eventos.application.AdicionarCategoria;
import br.pucrs.tcc.bilheteria.eventos.application.CriarEvento;
import br.pucrs.tcc.bilheteria.eventos.application.EventoService;
import br.pucrs.tcc.bilheteria.eventos.domain.Evento;
import br.pucrs.tcc.bilheteria.ingressos.application.IngressoService;
import br.pucrs.tcc.bilheteria.ingressos.domain.Ingresso;
import br.pucrs.tcc.bilheteria.ingressos.domain.StatusIngresso;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagamentoService;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagarReserva;
import br.pucrs.tcc.bilheteria.reservas.application.ReservaService;
import br.pucrs.tcc.bilheteria.reservas.application.ReservarIngressos;
import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;
import br.pucrs.tcc.bilheteria.usuarios.application.EmissorDeToken;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "bilheteria.reservas.expiracao-ativa=false",
                "spring.datasource.hikari.maximum-pool-size=20"
        })
class IngressosIntegracaoTest {

    @Autowired EventoService eventos;
    @Autowired ReservaService reservas;
    @Autowired PagamentoService pagamentos;
    @Autowired IngressoService ingressos;
    @Autowired Clock relogio;
    @Autowired EmissorDeToken emissor;

    @Value("${local.server.port}")
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private Reserva novaReserva(int quantidade) {
        Instant agora = relogio.instant();
        Evento e = eventos.criar(new CriarEvento("Show", "Arena", agora.plus(Duration.ofDays(30)), 1000));
        eventos.adicionarCategoria(e.id(), new AdicionarCategoria("Pista", 5000, 100,
                agora.minusSeconds(60), agora.plus(Duration.ofDays(29))));
        Evento publicado = eventos.publicar(e.id());
        return reservas.reservar(new ReservarIngressos(UUID.randomUUID(),
                publicado.categorias().get(0).id(), quantidade));
    }

    private void pagar(Reserva reserva, String token) {
        pagamentos.pagar(new PagarReserva(reserva.usuarioId(), reserva.id(),  UUID.randomUUID().toString(), token));
    }

    @Test
    void pagamentoAprovadoEmiteUmIngressoPorUnidade() {
        Reserva reserva = novaReserva(3);
        assertThat(ingressos.listarPorReserva(reserva.id())).isEmpty();

        pagar(reserva, "tok-ok");

        List<Ingresso> emitidos = ingressos.listarPorReserva(reserva.id());
        assertThat(emitidos).extracting(Ingresso::numero).containsExactly(1, 2, 3);
        assertThat(emitidos).extracting(Ingresso::codigo).doesNotHaveDuplicates();
        assertThat(emitidos).allMatch(i -> i.status() == StatusIngresso.EMITIDO);
        assertThat(emitidos).allMatch(i -> i.usuarioId().equals(reserva.usuarioId()));
    }

    @Test
    void pagamentoRecusadoNaoEmiteIngressos() {
        Reserva reserva = novaReserva(2);

        pagar(reserva, "cartao-recusado");

        assertThat(ingressos.listarPorReserva(reserva.id())).isEmpty();
    }

    @Test
    void dezCheckInsSimultaneosDoMesmoCodigoPassamUmaUnicaVez() throws Exception {
        Reserva reserva = novaReserva(1);
        pagar(reserva, "tok-ok");
        String codigo = ingressos.listarPorReserva(reserva.id()).get(0).codigo();
        int tentativas = 10;

        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger aceitos = new AtomicInteger();
        AtomicInteger recusados = new AtomicInteger();
        AtomicInteger outrosErros = new AtomicInteger();

        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            Callable<Void> leitura = () -> {
                largada.await();
                try {
                    ingressos.realizarCheckIn(codigo);
                    aceitos.incrementAndGet();
                } catch (ConflitoException e) {
                    recusados.incrementAndGet();
                } catch (RuntimeException e) {
                    outrosErros.incrementAndGet();
                }
                return null;
            };
            futuros.add(pool.submit(leitura));
        }
        largada.countDown();
        for (Future<?> f : futuros) {
            f.get();
        }
        pool.shutdown();

        assertThat(outrosErros.get()).as("falhas inesperadas").isZero();
        assertThat(aceitos.get()).as("check-ins aceitos").isEqualTo(1);
        assertThat(recusados.get()).as("recusados como já utilizado").isEqualTo(9);
        assertThat(ingressos.buscarPorCodigo(codigo).status()).isEqualTo(StatusIngresso.UTILIZADO);
    }

    private HttpResponse<String> http(String metodo, String caminho, String jwt) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Authorization", "Bearer " + jwt);
        if (metodo.equals("POST")) {
            req.POST(BodyPublishers.noBody());
        } else {
            req.GET();
        }
        return http.send(req.build(), BodyHandlers.ofString());
    }

    @Test
    void apiConsultaEFazCheckIn() throws Exception {
        Reserva reserva = novaReserva(2);
        pagar(reserva, "tok-ok");
        String codigo = ingressos.listarPorReserva(reserva.id()).get(0).codigo();
        String dono = emissor.emitir(reserva.usuarioId(), Papel.CLIENTE).valor();
        String portaria = emissor.emitir(UUID.randomUUID(), Papel.PORTARIA).valor();

        HttpResponse<String> lista = http("GET", "/ingressos?reservaId=" + reserva.id(), dono);
        assertThat(lista.statusCode()).isEqualTo(200);
        assertThat(lista.body()).contains(codigo).contains("EMITIDO");

        HttpResponse<String> primeiro = http("POST", "/ingressos/" + codigo + "/check-in", portaria);
        assertThat(primeiro.statusCode()).isEqualTo(200);
        assertThat(primeiro.body()).contains("UTILIZADO");

        HttpResponse<String> segundo = http("POST", "/ingressos/" + codigo + "/check-in", portaria);
        assertThat(segundo.statusCode()).isEqualTo(409);
        assertThat(segundo.body()).contains("ingresso.ja-utilizado");

        assertThat(http("GET", "/ingressos/codigo-que-nao-existe", portaria).statusCode()).isEqualTo(404);
    }
}