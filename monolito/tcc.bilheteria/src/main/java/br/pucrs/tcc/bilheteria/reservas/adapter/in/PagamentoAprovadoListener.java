package br.pucrs.tcc.bilheteria.reservas.adapter.in;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.PagamentoAprovado;
import br.pucrs.tcc.bilheteria.reservas.application.ReservaService;

@Component
class PagamentoAprovadoListener {

    private final ReservaService service;

    PagamentoAprovadoListener(ReservaService service) {
        this.service = service;
    }

    @EventListener
    void aoAprovarPagamento(PagamentoAprovado evento) {
        service.confirmar(evento.reservaId());
    }
}