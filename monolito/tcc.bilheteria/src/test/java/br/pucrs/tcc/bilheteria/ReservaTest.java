package br.pucrs.tcc.bilheteria;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration VALIDADE = Duration.ofMinutes(15);
    private static final UUID USUARIO = UUID.randomUUID();

    private Disponibilidade disponibilidade(int capacidade) {
        return Disponibilidade.criar(UUID.randomUUID(), UUID.randomUUID(), "Pista", Dinheiro.deCentavos(5000),
                capacidade, Periodo.de(AGORA.minusSeconds(60), AGORA.plus(Duration.ofDays(10))));
    }

    @Test
    void criaReservaCalculandoValorEPrazo() {
        Disponibilidade d = disponibilidade(10);
        Reserva r = Reserva.criar(USUARIO, d, 4, AGORA, VALIDADE);

        assertThat(r.valorTotal()).isEqualTo(Dinheiro.deCentavos(20000));
        assertThat(r.expiraEm()).isEqualTo(AGORA.plus(VALIDADE));
        assertThat(r.status()).isEqualTo(StatusReserva.AGUARDANDO_PAGAMENTO);
        assertThat(d.reservados()).isEqualTo(4);
    }

    @Test
    void rejeitaQuantidadeForaDoLimite() {
        Disponibilidade d = disponibilidade(50);
        assertThatThrownBy(() -> Reserva.criar(USUARIO, d, 0, AGORA, VALIDADE))
                .hasFieldOrPropertyWithValue("codigo", "reserva.quantidade-invalida");
        assertThatThrownBy(() -> Reserva.criar(USUARIO, d, 11, AGORA, VALIDADE))
                .hasFieldOrPropertyWithValue("codigo", "reserva.quantidade-invalida");
        assertThat(d.reservados()).isZero();
    }

    @Test
    void rejeitaQuandoEsgotadoSemAlterarOEstoque() {
        Disponibilidade d = disponibilidade(5);
        Reserva.criar(USUARIO, d, 4, AGORA, VALIDADE);
        assertThatThrownBy(() -> Reserva.criar(USUARIO, d, 2, AGORA, VALIDADE))
                .hasFieldOrPropertyWithValue("codigo", "disponibilidade.esgotada");
        assertThat(d.reservados()).isEqualTo(4);
    }

    @Test
    void rejeitaVendaForaDaJanela() {
        Disponibilidade d = disponibilidade(10);
        Instant depois = AGORA.plus(Duration.ofDays(11));
        assertThatThrownBy(() -> Reserva.criar(USUARIO, d, 1, depois, VALIDADE))
                .hasFieldOrPropertyWithValue("codigo", "venda.fora-da-janela");
    }

    @Test
    void naoExpiraAntesDoPrazo() {
        Disponibilidade d = disponibilidade(10);
        Reserva r = Reserva.criar(USUARIO, d, 3, AGORA, VALIDADE);

        assertThat(r.expirar(AGORA.plus(VALIDADE).minusSeconds(1), d)).isFalse();
        assertThat(r.status()).isEqualTo(StatusReserva.AGUARDANDO_PAGAMENTO);
        assertThat(d.reservados()).isEqualTo(3);
    }

    @Test
    void expiraNoPrazoDevolvendoIngressosUmaUnicaVez() {
        Disponibilidade d = disponibilidade(10);
        Reserva r = Reserva.criar(USUARIO, d, 3, AGORA, VALIDADE);

        assertThat(r.expirar(AGORA.plus(VALIDADE), d)).isTrue();
        assertThat(d.reservados()).isZero();
        assertThat(r.expirar(AGORA.plus(VALIDADE), d)).isFalse(); // idempotente
        assertThat(d.reservados()).isZero();
    }

    @Test
    void confirmaDentroDoPrazoEEhIdempotente() {
        Disponibilidade d = disponibilidade(10);
        Reserva r = Reserva.criar(USUARIO, d, 2, AGORA, VALIDADE);

        r.confirmar(AGORA.plusSeconds(60));
        r.confirmar(AGORA.plusSeconds(61));
        assertThat(r.status()).isEqualTo(StatusReserva.CONFIRMADA);
    }

    @Test
    void naoConfirmaDepoisDoPrazoNemExpiraConfirmada() {
        Disponibilidade d = disponibilidade(10);
        Reserva atrasada = Reserva.criar(USUARIO, d, 2, AGORA, VALIDADE);
        assertThatThrownBy(() -> atrasada.confirmar(AGORA.plus(VALIDADE)))
                .hasFieldOrPropertyWithValue("codigo", "reserva.expirada");

        Reserva paga = Reserva.criar(USUARIO, d, 2, AGORA, VALIDADE);
        paga.confirmar(AGORA.plusSeconds(10));
        assertThat(paga.expirar(AGORA.plus(Duration.ofHours(1)), d)).isFalse();
    }
}