package br.pucrs.tcc.bilheteria.reservas.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.reservas.domain.StatusReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva", schema = "reservas")
class ReservaJpa {

    @Id
    UUID id;

    @Column(name = "usuario_id")
    UUID usuarioId;

    @Column(name = "evento_id")
    UUID eventoId;

    @Column(name = "categoria_id")
    UUID categoriaId;

    int quantidade;

    @Column(name = "valor_total_centavos")
    long valorTotalCentavos;

    @Enumerated(EnumType.STRING)
    StatusReserva status;

    @Column(name = "criada_em")
    Instant criadaEm;

    @Column(name = "expira_em")
    Instant expiraEm;

    protected ReservaJpa() {
    }
}