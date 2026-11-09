package br.pucrs.tcc.bilheteria;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.PagamentoAprovado;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaConfirmada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaCriada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaExpirada;
import br.pucrs.tcc.bilheteria.compartilhado.infra.metricas.MetricasDeNegocio;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MetricasDeNegocioTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final SimpleMeterRegistry registro = new SimpleMeterRegistry();
    private final MetricasDeNegocio metricas = new MetricasDeNegocio(registro);

    private double valor(String nome) {
        return registro.get(nome).counter().count();
    }

    @Test
    void contadoresNascemZerados() {
        assertThat(valor("bilheteria.reservas.criadas")).isZero();
        assertThat(valor("bilheteria.ingressos.confirmados")).isZero();
    }

    @Test
    void cadaEventoIncrementaOSeuContador() {
        UUID id = UUID.randomUUID();

        metricas.aoCriarReserva(new ReservaCriada(id, T0, id, id, id, 1000, T0));
        metricas.aoCriarReserva(new ReservaCriada(id, T0, id, id, id, 1000, T0));
        metricas.aoExpirarReserva(new ReservaExpirada(id, T0, id));
        metricas.aoAprovarPagamento(new PagamentoAprovado(id, T0, id, id, 1000));

        assertThat(valor("bilheteria.reservas.criadas")).isEqualTo(2);
        assertThat(valor("bilheteria.reservas.expiradas")).isEqualTo(1);
        assertThat(valor("bilheteria.pagamentos.aprovados")).isEqualTo(1);
    }

    @Test
    void reservaConfirmadaSomaAQuantidadeDeIngressos() {
        UUID id = UUID.randomUUID();

        metricas.aoConfirmarReserva(new ReservaConfirmada(id, T0, id, id, id, id, 3));
        metricas.aoConfirmarReserva(new ReservaConfirmada(id, T0, id, id, id, id, 2));

        assertThat(valor("bilheteria.reservas.confirmadas")).isEqualTo(2);
        assertThat(valor("bilheteria.ingressos.confirmados")).isEqualTo(5);
    }
}