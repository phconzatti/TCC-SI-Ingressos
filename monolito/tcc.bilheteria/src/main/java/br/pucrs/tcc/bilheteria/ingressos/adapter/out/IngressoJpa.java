package br.pucrs.tcc.bilheteria.ingressos.adapter.out;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.ingressos.domain.StatusIngresso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingresso", schema = "ingressos")
class IngressoJpa {

    @Id
    UUID id;

    @Column(name = "reserva_id")
    UUID reservaId;

    int numero;

    @Column(name = "evento_id")
    UUID eventoId;

    @Column(name = "categoria_id")
    UUID categoriaId;

    @Column(name = "usuario_id")
    UUID usuarioId;

    String codigo;

    @Enumerated(EnumType.STRING)
    StatusIngresso status;

    @Column(name = "emitido_em")
    Instant emitidoEm;

    @Column(name = "utilizado_em")
    Instant utilizadoEm;

    protected IngressoJpa() {
    }
}