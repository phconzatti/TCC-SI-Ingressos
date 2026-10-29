package br.pucrs.tcc.bilheteria.reservas.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "disponibilidade", schema = "reservas")
class DisponibilidadeJpa {

    @Id
    UUID id;

    @Column(name = "evento_id")
    UUID eventoId;

    @Column(name = "nome_categoria")
    String nomeCategoria;

    @Column(name = "preco_centavos")
    long precoCentavos;

    int capacidade;

    int reservados;

    @Column(name = "inicio_vendas")
    Instant inicioVendas;

    @Column(name = "fim_vendas")
    Instant fimVendas;

    protected DisponibilidadeJpa() {
    }
}