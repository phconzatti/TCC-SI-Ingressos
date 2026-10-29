package br.pucrs.tcc.bilheteria;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.eventos.application.AdicionarCategoria;
import br.pucrs.tcc.bilheteria.eventos.application.CriarEvento;
import br.pucrs.tcc.bilheteria.eventos.application.EventoService;
import br.pucrs.tcc.bilheteria.eventos.domain.Evento;
import br.pucrs.tcc.bilheteria.reservas.application.ReservaService;
import br.pucrs.tcc.bilheteria.reservas.application.ReservarIngressos;
import br.pucrs.tcc.bilheteria.reservas.domain.Disponibilidade;
import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;
import br.pucrs.tcc.bilheteria.reservas.domain.StatusReserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

@Import({TestcontainersConfiguration.class, ReservasIntegracaoTest.RelogioControlavel.class})
@SpringBootTest(properties = "bilheteria.reservas.expiracao-ativa=false")
class ReservasIntegracaoTest {

    /** Relógio que o teste avança à mão, para simular a passagem do tempo sem esperar. */
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
    @Autowired Mutavel relogio;

    /** Cria e publica um evento com uma categoria; devolve [eventoId, categoriaId]. */
    private UUID[] eventoPublicadoComCategoria(int capacidadeDaCategoria) {
        Instant agora = relogio.instant();
        Evento e = eventos.criar(new CriarEvento("Show", "Arena", agora.plus(Duration.ofDays(30)), 1000));
        eventos.adicionarCategoria(e.id(), new AdicionarCategoria("Pista", 5000, capacidadeDaCategoria,
                agora.minusSeconds(60), agora.plus(Duration.ofDays(29))));
        Evento publicado = eventos.publicar(e.id());
        return new UUID[]{publicado.id(), publicado.categorias().get(0).id()};
    }

    private Disponibilidade disponibilidade(UUID eventoId) {
        return reservas.disponibilidadesDoEvento(eventoId).get(0);
    }

    @Test
    void publicarEventoCriaDisponibilidadeEmReservas() {
        UUID[] ids = eventoPublicadoComCategoria(10);

        Disponibilidade d = disponibilidade(ids[0]);
        assertThat(d.id()).isEqualTo(ids[1]);
        assertThat(d.capacidade()).isEqualTo(10);
        assertThat(d.reservados()).isZero();
    }

    @Test
    void cinquentaComprasSimultaneasParaDezIngressosVendemExatamenteDez() throws Exception {
        UUID[] ids = eventoPublicadoComCategoria(10);
        int tentativas = 50;

        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger vendas = new AtomicInteger();
        AtomicInteger esgotadas = new AtomicInteger();
        AtomicInteger outrosErros = new AtomicInteger();

        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            Callable<Void> compra = () -> {
                largada.await();
                try {
                    reservas.reservar(new ReservarIngressos(UUID.randomUUID(), ids[1], 1));
                    vendas.incrementAndGet();
                } catch (ConflitoException e) {
                    esgotadas.incrementAndGet();
                } catch (RuntimeException e) {
                    outrosErros.incrementAndGet();
                }
                return null;
            };
            futuros.add(pool.submit(compra));
        }
        largada.countDown();
        for (Future<?> f : futuros) {
            f.get();
        }
        pool.shutdown();

        assertThat(outrosErros.get()).as("falhas inesperadas").isZero();
        assertThat(vendas.get()).as("ingressos vendidos").isEqualTo(10);
        assertThat(esgotadas.get()).as("recusados por esgotamento").isEqualTo(40);
        assertThat(disponibilidade(ids[0]).reservados()).isEqualTo(10);
    }

    @Test
    void reservaVencidaExpiraEDevolveOsIngressos() {
        UUID[] ids = eventoPublicadoComCategoria(10);
        Reserva r = reservas.reservar(new ReservarIngressos(UUID.randomUUID(), ids[1], 3));
        assertThat(disponibilidade(ids[0]).reservados()).isEqualTo(3);

        assertThat(reservas.expirar(r.id())).as("antes do prazo").isFalse();

        relogio.avancar(Duration.ofMinutes(16));
        assertThat(reservas.reservasVencidas()).contains(r.id());
        assertThat(reservas.expirar(r.id())).isTrue();

        assertThat(reservas.buscar(r.id()).status()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(disponibilidade(ids[0]).reservados()).isZero();
        assertThat(reservas.expirar(r.id())).as("segunda vez é no-op").isFalse();
    }
} {
    
}
