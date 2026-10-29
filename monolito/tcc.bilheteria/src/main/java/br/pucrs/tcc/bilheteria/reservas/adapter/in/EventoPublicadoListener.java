package br.pucrs.tcc.bilheteria.reservas.adapter.in;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.EventoPublicado;

import br.pucrs.tcc.bilheteria.reservas.application.ReservaService;

@Component
class EventoPublicadoListener {
    private final ReservaService service;

    EventoPublicadoListener(ReservaService service) {
        this.service = service;
    }

    @EventListener
    void aoPublicar(EventoPublicado evento) {
        service.registrarDisponibilidades(evento);
    }
}
