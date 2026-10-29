package br.pucrs.tcc.bilheteria.reservas.adapter.in;

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
