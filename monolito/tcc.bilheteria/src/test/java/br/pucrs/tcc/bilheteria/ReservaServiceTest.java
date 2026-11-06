package br.pucrs.tcc.bilheteria;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.EventoPublicado;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaConfirmada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaCriada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaExpirada;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;
import br.pucrs.tcc.bilheteria.reservas.application.RepositorioDeDisponibilidades;
import br.pucrs.tcc.bilheteria.reservas.application.RepositorioDeReservas;
import br.pucrs.tcc.bilheteria.reservas.application.ReservaService;
import br.pucrs.tcc.bilheteria.reservas.application.ReservarIngressos;
import br.pucrs.tcc.bilheteria.reservas.domain.Disponibilidade;
import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;
import br.pucrs.tcc.bilheteria.reservas.domain.StatusReserva;

class ReservaServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final Map<UUID, Disponibilidade> disponibilidades = new HashMap<>();
    private final Map<UUID, Reserva> reservasSalvas = new HashMap<>();
    private final List<EventoDeIntegracao> publicados = new ArrayList<>();
    private Instant agora = T0;

    private final Clock relogio = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    };

    private final RepositorioDeDisponibilidades repositorioDeDisponibilidades = new RepositorioDeDisponibilidades() {
        public boolean existe(UUID id) { return disponibilidades.containsKey(id); }
        public void salvar(Disponibilidade d) { disponibilidades.put(d.id(), d); }
        public Optional<Disponibilidade> buscarParaAtualizacao(UUID id) {
            return Optional.ofNullable(disponibilidades.get(id));
        }
        public List<Disponibilidade> listarPorEvento(UUID eventoId) {
            return disponibilidades.values().stream().filter(d -> d.eventoId().equals(eventoId)).toList();
        }
    };

    private final RepositorioDeReservas repositorioDeReservas = new RepositorioDeReservas() {
        public void salvar(Reserva r) { reservasSalvas.put(r.id(), r); }
        public Optional<Reserva> buscar(UUID id) { return Optional.ofNullable(reservasSalvas.get(id)); }
        public Optional<Reserva> buscarParaAtualizacao(UUID id) { return buscar(id); }
        public List<UUID> idsVencidas(Instant limite, int quantidade) {
            return reservasSalvas.values().stream()
                    .filter(r -> r.status() == StatusReserva.AGUARDANDO_PAGAMENTO && !r.expiraEm().isAfter(limite))
                    .map(Reserva::id).toList();
        }
    };

    private final ReservaService service = new ReservaService(
            repositorioDeDisponibilidades, repositorioDeReservas, publicados::add, relogio, Duration.ofMinutes(15));

    private UUID categoriaPublicada(int capacidade) {
        UUID eventoId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        service.registrarDisponibilidades(new EventoPublicado(UUID.randomUUID(), T0, eventoId, "Show",
                T0.plus(Duration.ofDays(30)),
                List.of(new EventoPublicado.CategoriaPublicada(categoriaId, "Pista", 5000, capacidade,
                        T0.minusSeconds(60), T0.plus(Duration.ofDays(29))))));
        return categoriaId;
    }

    private long publicadosDoTipo(Class<?> tipo) {
        return publicados.stream().filter(tipo::isInstance).count();
    }

    @Test
    void registrarDisponibilidadesEhIdempotente() {
        UUID eventoId = UUID.randomUUID();
        EventoPublicado evento = new EventoPublicado(UUID.randomUUID(), T0, eventoId, "Show",
                T0.plus(Duration.ofDays(30)),
                List.of(new EventoPublicado.CategoriaPublicada(UUID.randomUUID(), "Pista", 5000, 10,
                        T0.minusSeconds(60), T0.plus(Duration.ofDays(29)))));
        service.registrarDisponibilidades(evento);
        service.registrarDisponibilidades(evento);

        assertThat(disponibilidades).hasSize(1);
    }

    @Test
    void reservarConsomeEstoqueECalculaValorEPublicaReservaCriada() {
        UUID categoria = categoriaPublicada(10);

        Reserva r = service.reservar(new ReservarIngressos(UUID.randomUUID(), categoria, 4));

        assertThat(r.valorTotal().centavos()).isEqualTo(20000);
        assertThat(disponibilidades.get(categoria).reservados()).isEqualTo(4);
        assertThat(publicadosDoTipo(ReservaCriada.class)).isEqualTo(1);
    }

    @Test
    void reservarSemEstoqueNaoAlteraEstadoNemPublica() {
        UUID categoria = categoriaPublicada(5);
        service.reservar(new ReservarIngressos(UUID.randomUUID(), categoria, 4));

        assertThatThrownBy(() -> service.reservar(new ReservarIngressos(UUID.randomUUID(), categoria, 2)))
                .hasFieldOrPropertyWithValue("codigo", "disponibilidade.esgotada");

        assertThat(disponibilidades.get(categoria).reservados()).isEqualTo(4);
        assertThat(publicadosDoTipo(ReservaCriada.class)).isEqualTo(1);
    }

    @Test
    void expirarDevolveIngressosEPublicaUmaVez() {
        UUID categoria = categoriaPublicada(10);
        Reserva r = service.reservar(new ReservarIngressos(UUID.randomUUID(), categoria, 3));
        assertThat(service.expirar(r.id())).as("antes do prazo").isFalse();

        agora = T0.plus(Duration.ofMinutes(15));
        assertThat(service.reservasVencidas()).containsExactly(r.id());
        assertThat(service.expirar(r.id())).isTrue();
        assertThat(service.expirar(r.id())).as("segunda vez").isFalse();

        assertThat(disponibilidades.get(categoria).reservados()).isZero();
        assertThat(publicadosDoTipo(ReservaExpirada.class)).isEqualTo(1);
    }

    @Test
    void confirmarMarcaConfirmadaEPublicaReservaConfirmada() {
        UUID categoria = categoriaPublicada(10);
        Reserva r = service.reservar(new ReservarIngressos(UUID.randomUUID(), categoria, 2));

        Reserva confirmada = service.confirmar(r.id());

        assertThat(confirmada.status()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(publicados.stream().filter(ReservaConfirmada.class::isInstance)
                .map(ReservaConfirmada.class::cast))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.reservaId()).isEqualTo(r.id());
                    assertThat(e.quantidade()).isEqualTo(2);
                    assertThat(e.categoriaId()).isEqualTo(categoria);
                });
    }

    @Test
    void confirmarDeNovoEhIdempotenteESemSegundoEvento() {
        UUID categoria = categoriaPublicada(10);
        Reserva r = service.reservar(new ReservarIngressos(UUID.randomUUID(), categoria, 2));

        service.confirmar(r.id());
        service.confirmar(r.id());

        assertThat(publicadosDoTipo(ReservaConfirmada.class)).isEqualTo(1);
    }

    @Test
    void reservaConfirmadaNaoExpiraENaoDevolveIngressos() {
        UUID categoria = categoriaPublicada(10);
        Reserva r = service.reservar(new ReservarIngressos(UUID.randomUUID(), categoria, 2));
        service.confirmar(r.id());

        agora = T0.plus(Duration.ofHours(1));

        assertThat(service.reservasVencidas()).isEmpty();
        assertThat(service.expirar(r.id())).isFalse();
        assertThat(disponibilidades.get(categoria).reservados()).isEqualTo(2);
        assertThat(publicadosDoTipo(ReservaExpirada.class)).isZero();
    }

    @Test
    void confirmarReservaVencidaFalhaComReservaExpirada() {
        UUID categoria = categoriaPublicada(10);
        Reserva r = service.reservar(new ReservarIngressos(UUID.randomUUID(), categoria, 2));
        agora = T0.plus(Duration.ofMinutes(15));

        assertThatThrownBy(() -> service.confirmar(r.id()))
                .hasFieldOrPropertyWithValue("codigo", "reserva.expirada");
        assertThat(publicadosDoTipo(ReservaConfirmada.class)).isZero();
    }

    @Test
    void confirmarReservaInexistenteLancaNaoEncontrado() {
        assertThatThrownBy(() -> service.confirmar(UUID.randomUUID()))
                .hasFieldOrPropertyWithValue("codigo", "reserva.nao-encontrada");
    }

        @Test
    void soODonoConsultaAReserva() {
        UUID categoria = categoriaPublicada(10);
        UUID dono = UUID.randomUUID();
        Reserva r = service.reservar(new ReservarIngressos(dono, categoria, 1));

        assertThat(service.buscarDoUsuario(r.id(), dono).id()).isEqualTo(r.id());
        assertThatThrownBy(() -> service.buscarDoUsuario(r.id(), UUID.randomUUID()))
                .hasFieldOrPropertyWithValue("codigo", "reserva.de-outro-usuario");
    }
}