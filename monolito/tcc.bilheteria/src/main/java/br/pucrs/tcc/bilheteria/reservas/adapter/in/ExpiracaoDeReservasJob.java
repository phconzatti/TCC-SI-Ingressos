package br.pucrs.tcc.bilheteria.reservas.adapter.in;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.reservas.application.ReservaService;

@Component
@ConditionalOnProperty(name = "bilheteria.reservas.expiracao-ativa", havingValue = "true", matchIfMissing = true)
class ExpiracaoDeReservasJob {
    
    private static final Logger log = LoggerFactory.getLogger(ExpiracaoDeReservasJob.class);

    private final ReservaService service;

    ExpiracaoDeReservasJob(ReservaService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${bilheteria.reservas.intervalo-expiracao:PT30S}")
    void expirarVencidas() {
        for (UUID id : service.reservasVencidas()) {
            try {
                if (service.expirar(id)) {
                    log.info("Reserva {} expirada", id);
                }
            } catch (RuntimeException e) {
                log.warn("Falha ao expirar reserva {}", id, e);
            }
        }
    }
}
