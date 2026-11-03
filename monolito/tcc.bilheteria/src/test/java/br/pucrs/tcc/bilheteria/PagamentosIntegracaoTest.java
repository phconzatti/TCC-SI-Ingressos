package br.pucrs.tcc.bilheteria;

import br.pucrs.tcc.bilheteria.eventos.application.AdicionarCategoria;
import br.pucrs.tcc.bilheteria.eventos.application.CriarEvento;
import br.pucrs.tcc.bilheteria.eventos.application.EventoService;
import br.pucrs.tcc.bilheteria.eventos.domain.Evento;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagamentoService;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagarReserva;
import br.pucrs.tcc.bilheteria.pagamentos.application.ResultadoDoPagamento;
import br.pucrs.tcc.bilheteria.pagamentos.domain.StatusPagamento;
import br.pucrs.tcc.bilheteria.reservas.application.ReservaService;
import br.pucrs.tcc.bilheteria.reservas.application.ReservarIngressos;
import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({TestcontainersConfiguration.class, PagamentosIntegracaoTest.RelogioControlavel.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "bilheteria.reservas.expiracao-ativa=false")
class PagamentosIntegracaoTest {

    static class Mutavel extends Clock {
        private volatile Instant agora = Instant.now();

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
        void avancar(Duration d) { agora = agora.plus(d); }
    }

    @TestConfiguration
    static class RelogioControlavel {
        @Bean
        @Primary
        Mutavel relogioDeTeste() {
            return new Mutavel();
        }
    }

    @Autowired EventoService eventos;
    @Autowired ReservaService reservas;
    @Autowired PagamentoService pagamentos;
    @Autowired Mutavel relogio;

    @Value("${local.server.port}")
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private Reserva novaReserva(int quantidade) {
        Instant agora = relogio.instant();
        Evento e = eventos.criar(new CriarEvento("Show", "Arena", agora.plus(Duration.ofDays(30)), 1000));
        eventos.adicionarCategoria(e.id(), new AdicionarCategoria("Pista", 5000, 100,
                agora.minusSeconds(60), agora.plus(Duration.ofDays(29))));
        Evento publicado = eventos.publicar(e.id());
        UUID categoriaId = publicado.categorias().get(0).id();
        return reservas.reservar(new ReservarIngressos(UUID.randomUUID(), categoriaId, quantidade));
    }

    private static String chave() {
        return UUID.randomUUID().toString();
    }

    @Test
    void reservaCriadaPodeSerPagaComOValorDaReserva() {
        Reserva reserva = novaReserva(2);

        ResultadoDoPagamento r = pagamentos.pagar(new PagarReserva(reserva.id(), chave(), "tok-ok"));

        assertThat(r.novo()).isTrue();
        assertThat(r.pagamento().status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(r.pagamento().valor().centavos()).isEqualTo(10000);
    }

    @Test
    void repetirAChaveDevolveOMesmoPagamentoSemCobrarDeNovo() {
        Reserva reserva = novaReserva(1);
        String k = chave();

        ResultadoDoPagamento primeiro = pagamentos.pagar(new PagarReserva(reserva.id(), k, "tok-ok"));
        ResultadoDoPagamento repeticao = pagamentos.pagar(new PagarReserva(reserva.id(), k, "tok-ok"));

        assertThat(repeticao.novo()).isFalse();
        assertThat(repeticao.pagamento().id()).isEqualTo(primeiro.pagamento().id());
    }

    @Test
    void pagamentoRecusadoPermiteNovaTentativaComOutraChave() {
        Reserva reserva = novaReserva(1);

        ResultadoDoPagamento recusado = pagamentos.pagar(new PagarReserva(reserva.id(), chave(), "cartao-recusado"));
        assertThat(recusado.pagamento().status()).isEqualTo(StatusPagamento.RECUSADO);

        ResultadoDoPagamento aprovado = pagamentos.pagar(new PagarReserva(reserva.id(), chave(), "tok-ok"));
        assertThat(aprovado.pagamento().status()).isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void reservaVencidaNaoPodeSerPaga() {
        Reserva reserva = novaReserva(1);
        relogio.avancar(Duration.ofMinutes(16));

        assertThatThrownBy(() -> pagamentos.pagar(new PagarReserva(reserva.id(), chave(), "tok-ok")))
                .hasFieldOrPropertyWithValue("codigo", "reserva.expirada");
    }

    @Test
    void reservaExpiradaEstornaOPagamentoAprovado() {
        Reserva reserva = novaReserva(1);
        Pagamento_ pago = new Pagamento_(pagamentos.pagar(new PagarReserva(reserva.id(), chave(), "tok-ok")));

        relogio.avancar(Duration.ofMinutes(16));
        assertThat(reservas.expirar(reserva.id())).isTrue();

        assertThat(pagamentos.buscar(pago.id()).status()).isEqualTo(StatusPagamento.ESTORNADO);
    }

    private record Pagamento_(ResultadoDoPagamento resultado) {
        UUID id() { return resultado.pagamento().id(); }
    }

    private HttpResponse<String> postarPagamento(String chaveHeader, UUID reservaId, String token) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/pagamentos"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString("""
                        {"reservaId":"%s","tokenDePagamento":"%s"}""".formatted(reservaId, token)));
        if (chaveHeader != null) {
            req.header("Idempotency-Key", chaveHeader);
        }
        return http.send(req.build(), BodyHandlers.ofString());
    }

    @Test
    void apiRespondeComOsCodigosEsperadosDeIdempotencia() throws Exception {
        Reserva reserva = novaReserva(1);
        String k = chave();

        HttpResponse<String> primeira = postarPagamento(k, reserva.id(), "tok-ok");
        assertThat(primeira.statusCode()).isEqualTo(201);
        assertThat(primeira.body()).contains("APROVADO");

        HttpResponse<String> repeticao = postarPagamento(k, reserva.id(), "tok-ok");
        assertThat(repeticao.statusCode()).isEqualTo(200);
        assertThat(repeticao.body()).isEqualTo(primeira.body());

        HttpResponse<String> outraChave = postarPagamento(chave(), reserva.id(), "tok-ok");
        assertThat(outraChave.statusCode()).isEqualTo(409);
        assertThat(outraChave.body()).contains("pagamento.ja-existe");

        HttpResponse<String> semHeader = postarPagamento(null, reserva.id(), "tok-ok");
        assertThat(semHeader.statusCode()).isEqualTo(400);
    }
}