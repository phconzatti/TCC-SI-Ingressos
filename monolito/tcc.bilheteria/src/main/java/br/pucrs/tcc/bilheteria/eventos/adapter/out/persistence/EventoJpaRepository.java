package br.pucrs.tcc.bilheteria.eventos.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface EventoJpaRepository extends JpaRepository<EventoJpa, UUID> {
    
}
