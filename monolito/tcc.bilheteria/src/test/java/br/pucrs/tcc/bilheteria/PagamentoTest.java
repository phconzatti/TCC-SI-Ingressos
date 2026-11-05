package br.pucrs.tcc.bilheteria;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.pagamentos.domain.Pagamento;
import br.pucrs.tcc.bilheteria.pagamentos.domain.ReservaRef;
import br.pucrs.tcc.bilheteria.pagamentos.domain.StatusPagamento;

class PagamentoTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T00:00:00Z");

    private ReservaRef reserva() {
        return ReservaRef.criar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Dinheiro.deCentavos(20000), AGORA.plus(Duration.ofMinutes(15)));
    }

    @Test
    void iniciaComOValorDaReserva() {
        Pagamento p = Pagamento.iniciar(reserva(), "k1", AGORA);
        assertThat(p.status()).isEqualTo(StatusPagamento.INICIADO);
        assertThat(p.valor()).isEqualTo(Dinheiro.deCentavos(20000));
    }

    @Test
    void rejeitaChaveEmBranco() {
        assertThatThrownBy(() -> Pagamento.iniciar(reserva(), " ", AGORA))
                .hasFieldOrPropertyWithValue("codigo", "pagamento.chave-obrigatoria");
    }

    @Test
    void naoIniciaParaReservaVencidaNemMarcadaComoExpirada() {
        ReservaRef vencida = reserva();
        assertThatThrownBy(() -> Pagamento.iniciar(vencida, "k1", AGORA.plus(Duration.ofMinutes(15))))
                .hasFieldOrPropertyWithValue("codigo", "reserva.expirada");

        ReservaRef marcada = reserva();
        marcada.marcarExpirada();
        assertThatThrownBy(() -> Pagamento.iniciar(marcada, "k2", AGORA))
                .hasFieldOrPropertyWithValue("codigo", "reserva.expirada");
    }

    @Test
    void aprovaERecusaSoAPartirDeIniciado() {
        Pagamento aprovado = Pagamento.iniciar(reserva(), "k1", AGORA);
        aprovado.aprovar("GW-1");
        assertThat(aprovado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(aprovado.codigoNoGateway()).isEqualTo("GW-1");
        assertThatThrownBy(() -> aprovado.recusar("x"))
                .hasFieldOrPropertyWithValue("codigo", "pagamento.transicao-invalida");

        Pagamento recusado = Pagamento.iniciar(reserva(), "k2", AGORA);
        recusado.recusar("Cartão recusado");
        assertThat(recusado.status()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(recusado.motivo()).isEqualTo("Cartão recusado");
    }

    @Test
    void expiracaoCancelaIniciadoEEstornaAprovado() {
        Pagamento iniciado = Pagamento.iniciar(reserva(), "k1", AGORA);
        assertThat(iniciado.aoExpirarReserva()).isTrue();
        assertThat(iniciado.status()).isEqualTo(StatusPagamento.CANCELADO);

        Pagamento aprovado = Pagamento.iniciar(reserva(), "k2", AGORA);
        aprovado.aprovar("GW-2");
        assertThat(aprovado.aoExpirarReserva()).isTrue();
        assertThat(aprovado.status()).isEqualTo(StatusPagamento.ESTORNADO);
    }

    @Test
    void expiracaoEhIdempotenteEIgnoraEstadosFinais() {
        Pagamento p = Pagamento.iniciar(reserva(), "k1", AGORA);
        p.aprovar("GW-3");
        p.aoExpirarReserva();
        assertThat(p.aoExpirarReserva()).isFalse();
        assertThat(p.status()).isEqualTo(StatusPagamento.ESTORNADO);

        Pagamento recusado = Pagamento.iniciar(reserva(), "k2", AGORA);
        recusado.recusar("x");
        assertThat(recusado.aoExpirarReserva()).isFalse();
        assertThat(recusado.status()).isEqualTo(StatusPagamento.RECUSADO);
    }
}