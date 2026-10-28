package br.pucrs.tcc.bilheteria.compartilhado.contratos;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.EventoDeIntegracao;

// Emitido por Reservas quando uma reserva é criada. Pagamentos cria a ReservaRef local a partir dele
public record ReservaCriada(
    UUID id,
    Instant ocorridoEm,
    UUID reservaId,
    UUID usuarioId,
    UUID eventoId,
    long valorEmCentavos,
    Instant expiraEm) implements EventoDeIntegracao {}