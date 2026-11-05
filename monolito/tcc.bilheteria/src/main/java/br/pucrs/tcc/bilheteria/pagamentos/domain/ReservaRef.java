package br.pucrs.tcc.bilheteria.pagamentos.domain;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;

/*
Cópia local, dentro de Pagamentos, do que interessa de uma reserva: quanto cobrar e até quando
É criada a partir de ReservaCriada e atualizada por ReservaExpirada
Pagamentos não conhece a Reserva
*/
public class ReservaRef {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID eventoId;
    private final Dinheiro valor;
    private final Instant expiraEm;
    private boolean expirada;

    private ReservaRef(UUID id, UUID usuarioId, UUID eventoId, Dinheiro valor, Instant expiraEm, boolean expirada) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.eventoId = eventoId;
        this.valor = valor;
        this.expiraEm = expiraEm;
        this.expirada = expirada;
    }

    public static ReservaRef criar(UUID id, UUID usuarioId, UUID eventoId, Dinheiro valor, Instant expiraEm) {
        return new ReservaRef(id, usuarioId, eventoId, valor, expiraEm, false);
    }

    public static ReservaRef reconstituir(UUID id, UUID usuarioId, UUID eventoId, Dinheiro valor, Instant expiraEm, boolean expirada) {
        return new ReservaRef(id, usuarioId, eventoId, valor, expiraEm, expirada);
    }

    public boolean aceitaPagamento(Instant agora) {
        return !expirada && agora.isBefore(expiraEm);
    }

    public void marcarExpirada() {
        expirada = true;
    }

    public UUID id() { return id; }
    public UUID usuarioId() { return usuarioId; }
    public UUID eventoId() { return eventoId; }
    public Dinheiro valor() { return valor; }
    public Instant expiraEm() { return expiraEm; }
    public boolean expirada() { return expirada; }
}