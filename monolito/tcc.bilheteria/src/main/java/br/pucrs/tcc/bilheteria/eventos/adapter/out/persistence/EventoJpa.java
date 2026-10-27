package br.pucrs.tcc.bilheteria.eventos.adapter.out.persistence;

import br.pucrs.tcc.bilheteria.eventos.domain.StatusEvento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "evento", schema = "eventos")
class EventoJpa {
    
    @Id
    UUID id;

    String nome;

    @Column(name = "local_do_evento")
    String local;

    @Column(name = "data_do_evento")
    Instant dataDoEvento;

    int capacidade;

    @Enumerated(EnumType.STRING)
    StatusEvento status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "evento_id", nullable = false)
    List<CategoriaJpa> categorias = new ArrayList<>();

    protected EventoJpa() {}
}
