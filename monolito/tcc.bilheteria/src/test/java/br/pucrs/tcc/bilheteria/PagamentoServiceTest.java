package br.pucrs.tcc.bilheteria;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.PagamentoAprovado;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaCriada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaExpirada;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;
import br.pucrs.tcc.bilheteria.pagamentos.domain.Pagamento;
import br.pucrs.tcc.bilheteria.pagamentos.domain.ReservaRef;
import br.pucrs.tcc.bilheteria.pagamentos.domain.StatusPagamento;

import org.junit.jupiter.api.Test;

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

import br.pucrs.tcc.bilheteria.pagamentos.application.GatewayDePagamento;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagamentoService;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagarReserva;
import br.pucrs.tcc.bilheteria.pagamentos.application.RepositorioDePagamentos;
import br.pucrs.tcc.bilheteria.pagamentos.application.RepositorioDeReservasRef;
import br.pucrs.tcc.bilheteria.pagamentos.application.ResultadoDoPagamento;

class PagamentoServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final Map<UUID, ReservaRef> refs = new HashMap<>();
    private final Map<UUID, Pagamento> banco = new HashMap<>();
    private final List<EventoDeIntegracao> publicados = new ArrayList<>();
    private int cobrancas = 0;
    private Instant agora = T0;

    private final Clock relogio = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    };

    private final RepositorioDeReservasRef repositorioDeRefs = new RepositorioDeReservasRef() {
        public boolean existe(UUID id) { return refs.containsKey(id); }
        public void salvar(ReservaRef r) { refs.put(r.id(), r); }
        public Optional<ReservaRef> buscar(UUID id) { return Optional.ofNullable(refs.get(id)); }
    };

    private final RepositorioDePagamentos repositorioDePagamentos = new RepositorioDePagamentos() {
        public void salvar(Pagamento p) { banco.put(p.id(), p); }
        public Optional<Pagamento> buscar(UUID id) { return Optional.ofNullable(banco.get(id)); }
        public Optional<Pagamento> buscarPorChave(String c) {
            return banco.values().stream().filter(p -> p.chaveDeIdempotencia().equals(c)).findFirst();
        }
        public boolean existeAtivoParaReserva(UUID reservaId) {
            return banco.values().stream().anyMatch(p -> p.reservaId().equals(reservaId)
                    && (p.status() == StatusPagamento.INICIADO || p.status() == StatusPagamento.APROVADO));
        }
        public List<Pagamento> listarPorReserva(UUID reservaId) {
            return banco.values().stream().filter(p -> p.reservaId().equals(reservaId)).toList();
        }
    };

    private final GatewayDePagamento gateway = (id, valor, token) -> {
        cobrancas++;
        return token.equals("cartao-recusado")
                ? GatewayDePagamento.ResultadoDaCobranca.recusada("Cartão recusado")
                : GatewayDePagamento.ResultadoDaCobranca.aprovada("SIM-" + id);
    };

    private final PagamentoService service = new PagamentoService(
            repositorioDeRefs, repositorioDePagamentos, gateway, publicados::add, relogio);

    private UUID reservaConhecida(long valorEmCentavos) {
        UUID reservaId = UUID.randomUUID();
        service.registrarReserva(new ReservaCriada(UUID.randomUUID(), T0, reservaId, UUID.randomUUID(),
                UUID.randomUUID(), valorEmCentavos, T0.plus(Duration.ofMinutes(15))));
        return reservaId;
    }

    @Test
    void registrarReservaEhIdempotente() {
        UUID id = UUID.randomUUID();
        ReservaCriada evento = new ReservaCriada(UUID.randomUUID(), T0, id, UUID.randomUUID(),
                UUID.randomUUID(), 1000, T0.plus(Duration.ofMinutes(15)));
        service.registrarReserva(evento);
        service.registrarReserva(evento);
        assertThat(refs).hasSize(1);
    }

    @Test
    void pagaComOValorDaReservaEPublicaPagamentoAprovado() {
        UUID reserva = reservaConhecida(20000);

        ResultadoDoPagamento r = service.pagar(new PagarReserva(reserva, "k1", "tok-ok"));

        assertThat(r.novo()).isTrue();
        assertThat(r.pagamento().status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(r.pagamento().valor().centavos()).isEqualTo(20000);
        assertThat(publicados).singleElement().isInstanceOf(PagamentoAprovado.class);
    }

    @Test
    void repetirAMesmaChaveNaoCobraDeNovo() {
        UUID reserva = reservaConhecida(20000);
        ResultadoDoPagamento primeiro = service.pagar(new PagarReserva(reserva, "k1", "tok-ok"));
        ResultadoDoPagamento repeticao = service.pagar(new PagarReserva(reserva, "k1", "tok-ok"));

        assertThat(repeticao.novo()).isFalse();
        assertThat(repeticao.pagamento().id()).isEqualTo(primeiro.pagamento().id());
        assertThat(cobrancas).isEqualTo(1);
        assertThat(publicados).hasSize(1);
    }

    @Test
    void mesmaChaveEmOutraReservaEhConflito() {
        UUID a = reservaConhecida(1000);
        UUID b = reservaConhecida(1000);
        service.pagar(new PagarReserva(a, "k1", "tok-ok"));

        assertThatThrownBy(() -> service.pagar(new PagarReserva(b, "k1", "tok-ok")))
                .hasFieldOrPropertyWithValue("codigo", "idempotencia.chave-reutilizada");
    }

    @Test
    void naoPagaDuasVezesAMesmaReservaComChavesDiferentes() {
        UUID reserva = reservaConhecida(1000);
        service.pagar(new PagarReserva(reserva, "k1", "tok-ok"));

        assertThatThrownBy(() -> service.pagar(new PagarReserva(reserva, "k2", "tok-ok")))
                .hasFieldOrPropertyWithValue("codigo", "pagamento.ja-existe");
        assertThat(cobrancas).isEqualTo(1);
    }

    @Test
    void recusadoNaoPublicaEventoEPermiteNovaTentativa() {
        UUID reserva = reservaConhecida(1000);

        ResultadoDoPagamento recusado = service.pagar(new PagarReserva(reserva, "k1", "cartao-recusado"));
        assertThat(recusado.pagamento().status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(recusado.pagamento().motivo()).isEqualTo("Cartão recusado");
        assertThat(publicados).isEmpty();

        ResultadoDoPagamento novaTentativa = service.pagar(new PagarReserva(reserva, "k2", "tok-ok"));
        assertThat(novaTentativa.pagamento().status()).isEqualTo(StatusPagamento.APROVADO);
    }

    @Test
    void reservaDesconhecidaDevolveConflitoDeTentarNovamente() {
        assertThatThrownBy(() -> service.pagar(new PagarReserva(UUID.randomUUID(), "k1", "tok-ok")))
                .hasFieldOrPropertyWithValue("codigo", "reserva.indisponivel-para-pagamento");
    }

    @Test
    void reservaVencidaNaoCobraNoGateway() {
        UUID reserva = reservaConhecida(1000);
        agora = T0.plus(Duration.ofMinutes(15));

        assertThatThrownBy(() -> service.pagar(new PagarReserva(reserva, "k1", "tok-ok")))
                .hasFieldOrPropertyWithValue("codigo", "reserva.expirada");
        assertThat(cobrancas).isZero();
    }

    @Test
    void reservaExpiradaEstornaPagamentoAprovadoEBloqueiaNovos() {
        UUID reserva = reservaConhecida(1000);
        Pagamento pago = service.pagar(new PagarReserva(reserva, "k1", "tok-ok")).pagamento();

        service.aoReservaExpirar(new ReservaExpirada(UUID.randomUUID(), T0, reserva));
        service.aoReservaExpirar(new ReservaExpirada(UUID.randomUUID(), T0, reserva)); // repetição

        assertThat(banco.get(pago.id()).status()).isEqualTo(StatusPagamento.ESTORNADO);
        assertThat(refs.get(reserva).expirada()).isTrue();
        assertThatThrownBy(() -> service.pagar(new PagarReserva(reserva, "k2", "tok-ok")))
                .hasFieldOrPropertyWithValue("codigo", "reserva.expirada");
    }
}