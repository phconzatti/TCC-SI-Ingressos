package br.pucrs.tcc.bilheteria.compartilhado.contratos;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;

import java.time.Instant;
import java.util.UUID;

/* 
Emitido por Pagamentos quando um pagamento é aprovado 
Reservas confirma a reserva 
*/
public record PagamentoAprovado(
        UUID id,
        Instant ocorridoEm,
        UUID pagamentoId,
        UUID reservaId,
        long valorEmCentavos) implements EventoDeIntegracao {
}