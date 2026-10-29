package br.pucrs.tcc.bilheteria.reservas.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface DisponibilidadeJpaRepository extends JpaRepository<DisponibilidadeJpa, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DisponibilidadeJpa d where d.id = :id")
    Optional<DisponibilidadeJpa> buscarParaAtualizacao(@Param("id") UUID id);

    List<DisponibilidadeJpa> findByEventoId(UUID eventoId);
}