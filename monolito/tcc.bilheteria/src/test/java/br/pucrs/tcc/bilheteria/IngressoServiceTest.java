package br.pucrs.tcc.bilheteria;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaConfirmada;
import br.pucrs.tcc.bilheteria.ingressos.application.IngressoService;
import br.pucrs.tcc.bilheteria.ingressos.application.RepositorioDeIngressos;
import br.pucrs.tcc.bilheteria.ingressos.domain.Ingresso;
import br.pucrs.tcc.bilheteria.ingressos.domain.StatusIngresso;

class IngressoServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final Map<UUID, Ingresso> banco = new LinkedHashMap<>();
    private final AtomicInteger sequencia = new AtomicInteger();

    private final RepositorioDeIngressos repositorio = new RepositorioDeIngressos() {
        public void salvarTodos(List<Ingresso> lista) { lista.forEach(i -> banco.put(i.id(), i)); }
        public void salvar(Ingresso i) { banco.put(i.id(), i); }
        public boolean existePorReserva(UUID reservaId) {
            return banco.values().stream().anyMatch(i -> i.reservaId().equals(reservaId));
        }
        public List<Ingresso> listarPorReserva(UUID reservaId) {
            return banco.values().stream().filter(i -> i.reservaId().equals(reservaId))
                    .sorted(Comparator.comparingInt(Ingresso::numero)).toList();
        }
        public Optional<Ingresso> buscarPorCodigo(String codigo) {
            return banco.values().stream().filter(i -> i.codigo().equals(codigo)).findFirst();
        }
        public Optional<Ingresso> buscarPorCodigoParaAtualizacao(String codigo) {
            return buscarPorCodigo(codigo);
        }
    };

    private final IngressoService service = new IngressoService(
            repositorio, () -> "COD-" + sequencia.incrementAndGet(), Clock.fixed(T0, ZoneOffset.UTC));

    private ReservaConfirmada confirmada(UUID reservaId, int quantidade) {
        return new ReservaConfirmada(UUID.randomUUID(), T0, reservaId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), quantidade);
    }

    @Test
    void emiteUmIngressoPorUnidadeNumeradosECodigosDistintos() {
        UUID reserva = UUID.randomUUID();

        service.emitirIngressos(confirmada(reserva, 3));

        List<Ingresso> emitidos = service.listarPorReserva(reserva);
        assertThat(emitidos).extracting(Ingresso::numero).containsExactly(1, 2, 3);
        assertThat(emitidos).extracting(Ingresso::codigo).doesNotHaveDuplicates();
        assertThat(emitidos).allMatch(i -> i.status() == StatusIngresso.EMITIDO);
    }

    @Test
    void herdaOsDadosDaReservaConfirmada() {
        UUID reserva = UUID.randomUUID();
        ReservaConfirmada evento = confirmada(reserva, 1);

        service.emitirIngressos(evento);

        Ingresso i = service.listarPorReserva(reserva).get(0);
        assertThat(i.usuarioId()).isEqualTo(evento.usuarioId());
        assertThat(i.eventoId()).isEqualTo(evento.eventoId());
        assertThat(i.categoriaId()).isEqualTo(evento.categoriaId());
    }

    @Test
    void receberOMesmoEventoDuasVezesNaoDuplicaIngressos() {
        UUID reserva = UUID.randomUUID();
        ReservaConfirmada evento = confirmada(reserva, 2);

        service.emitirIngressos(evento);
        service.emitirIngressos(evento);

        assertThat(service.listarPorReserva(reserva)).hasSize(2);
    }

    @Test
    void checkInSoPassaUmaVez() {
        UUID reserva = UUID.randomUUID();
        service.emitirIngressos(confirmada(reserva, 2));
        List<Ingresso> emitidos = service.listarPorReserva(reserva);
        String codigo = emitidos.get(0).codigo();

        Ingresso feito = service.realizarCheckIn(codigo);

        assertThat(feito.status()).isEqualTo(StatusIngresso.UTILIZADO);
        assertThatThrownBy(() -> service.realizarCheckIn(codigo))
                .hasFieldOrPropertyWithValue("codigo", "ingresso.ja-utilizado");
        assertThat(service.buscarPorCodigo(emitidos.get(1).codigo()).status())
                .as("o outro ingresso da reserva segue válido").isEqualTo(StatusIngresso.EMITIDO);
    }

    @Test
    void codigoInexistenteLancaNaoEncontrado() {
        assertThatThrownBy(() -> service.realizarCheckIn("nao-existe"))
                .hasFieldOrPropertyWithValue("codigo", "ingresso.nao-encontrado");
        assertThatThrownBy(() -> service.buscarPorCodigo("nao-existe"))
                .hasFieldOrPropertyWithValue("codigo", "ingresso.nao-encontrado");
    }
}