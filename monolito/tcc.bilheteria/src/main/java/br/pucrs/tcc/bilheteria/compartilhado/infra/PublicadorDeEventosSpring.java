package br.pucrs.tcc.bilheteria.compartilhado.infra;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.PublicadorDeEventos;

// Adaptador do monólito: entrega síncrona, dentro da transação de quem publica
@Component
class PublicadorDeEventosSpring implements PublicadorDeEventos {
    
    private final ApplicationEventPublisher publicador;

    PublicadorDeEventosSpring(ApplicationEventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(EventoDeIntegracao evento) {
        publicador.publishEvent(evento);
    }
}
