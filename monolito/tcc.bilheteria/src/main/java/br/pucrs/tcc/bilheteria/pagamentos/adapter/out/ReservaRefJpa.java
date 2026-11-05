package br.pucrs.tcc.bilheteria.pagamentos.adapter.out;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva_ref", schema = "pagamentos")
class ReservaRefJpa {

    @Id
    UUID id;

    @Column(name = "usuario_id")
    UUID usuarioId;

    @Column(name = "evento_id")
    UUID eventoId;

    @Column(name = "valor_centavos")
    long valorCentavos;

    @Column(name = "expira_em")
    Instant expiraEm;

    boolean expirada;

    protected ReservaRefJpa() {
    }
}