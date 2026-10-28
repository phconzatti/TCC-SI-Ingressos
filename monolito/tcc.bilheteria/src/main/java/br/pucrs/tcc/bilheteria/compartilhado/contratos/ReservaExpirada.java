package br.pucrs.tcc.bilheteria.compartilhado.contratos;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;


// Emitido por Reservas quando o prazo de pagamento vence. Pagamentos cancela ou estorna
public record ReservaExpirada (
    UUID id, 
    Instant ocorridoEm,
    UUID reservaId) implements EventoDeIntegracao {  
}
