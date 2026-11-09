package br.pucrs.tcc.bilheteria.compartilhado.infra.metricas;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.PagamentoAprovado;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaConfirmada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaCriada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaExpirada;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class MetricasDeNegocio {

    private final Counter reservasCriadas;
    private final Counter reservasExpiradas;
    private final Counter pagamentosAprovados;
    private final Counter reservasConfirmadas;
    private final Counter ingressosConfirmados;

    public MetricasDeNegocio(MeterRegistry registro) {
        this.reservasCriadas = Counter.builder("bilheteria.reservas.criadas")
                .description("Reservas criadas").register(registro);
        this.reservasExpiradas = Counter.builder("bilheteria.reservas.expiradas")
                .description("Reservas expiradas sem pagamento").register(registro);
        this.pagamentosAprovados = Counter.builder("bilheteria.pagamentos.aprovados")
                .description("Pagamentos aprovados").register(registro);
        this.reservasConfirmadas = Counter.builder("bilheteria.reservas.confirmadas")
                .description("Reservas confirmadas após pagamento").register(registro);
        this.ingressosConfirmados = Counter.builder("bilheteria.ingressos.confirmados")
                .description("Ingressos vendidos (soma das quantidades das reservas confirmadas)")
                .register(registro);
    }

    @EventListener
    public void aoCriarReserva(ReservaCriada evento) {
        reservasCriadas.increment();
    }

    @EventListener
    public void aoExpirarReserva(ReservaExpirada evento) {
        reservasExpiradas.increment();
    }

    @EventListener
    public void aoAprovarPagamento(PagamentoAprovado evento) {
        pagamentosAprovados.increment();
    }

    @EventListener
    public void aoConfirmarReserva(ReservaConfirmada evento) {
        reservasConfirmadas.increment();
        ingressosConfirmados.increment(evento.quantidade());
    }
}