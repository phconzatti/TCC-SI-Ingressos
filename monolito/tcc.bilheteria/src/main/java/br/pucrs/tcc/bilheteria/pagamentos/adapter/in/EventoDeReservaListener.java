package br.pucrs.tcc.bilheteria.pagamentos.adapter.in;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaCriada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaExpirada;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagamentoService;

@Component
class EventosDeReservaListener {

    private final PagamentoService service;

    EventosDeReservaListener(PagamentoService service) {
        this.service = service;
    }

    @EventListener
    void aoCriarReserva(ReservaCriada evento) {
        service.registrarReserva(evento);
    }

    @EventListener
    void aoExpirarReserva(ReservaExpirada evento) {
        service.aoReservaExpirar(evento);
    }
}