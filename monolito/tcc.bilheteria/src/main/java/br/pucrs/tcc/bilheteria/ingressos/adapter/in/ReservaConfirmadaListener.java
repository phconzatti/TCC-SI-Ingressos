package br.pucrs.tcc.bilheteria.ingressos.adapter.in;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaConfirmada;
import br.pucrs.tcc.bilheteria.ingressos.application.IngressoService;

@Component
class ReservaConfirmadaListener {

    private final IngressoService service;

    ReservaConfirmadaListener(IngressoService service) {
        this.service = service;
    }

    // Mesma transação de quem pagou: se a emissão falhar, a confirmação e o pagamento são desfeitos
    @EventListener
    void aoConfirmarReserva(ReservaConfirmada evento) {
        service.emitirIngressos(evento);
    }
}