package br.pucrs.tcc.bilheteria.compartilhado.contratos;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;

/* 
Emitido por Reservas quando o pagamento confirma a reserva
Ingressos emite os ingressos
*/
public record ReservaConfirmada(
        UUID id,
        Instant ocorridoEm,
        UUID reservaId,
        UUID usuarioId,
        UUID eventoId,
        UUID categoriaId,
        int quantidade) implements EventoDeIntegracao {
}