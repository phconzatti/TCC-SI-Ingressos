package br.pucrs.tcc.bilheteria.eventos.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria", schema = "eventos")
class CategoriaJpa {
    
    @Id
    UUID id;

    String nome;

    @Column(name = "preco_centavos")
    long precoCentavos;

    int capacidade;

    @Column(name = "inicio_vendas")
    Instant inicioVendas;

    @Column(name = "fim_vendas")
    Instant fimVendas;

    protected CategoriaJpa() {}
}
