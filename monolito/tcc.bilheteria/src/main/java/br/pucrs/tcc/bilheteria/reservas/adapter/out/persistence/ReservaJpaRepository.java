package br.pucrs.tcc.bilheteria.reservas.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.pucrs.tcc.bilheteria.reservas.domain.StatusReserva;
import jakarta.persistence.LockModeType;

interface ReservaJpaRepository extends JpaRepository<ReservaJpa, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservaJpa r where r.id = :id")
    Optional<ReservaJpa> buscarParaAtualizacao(@Param("id") UUID id);

    @Query("select r.id from ReservaJpa r where r.status = :status and r.expiraEm <= :agora order by r.expiraEm")
    List<UUID> idsVencidas(@Param("status") StatusReserva status, @Param("agora") Instant agora, Pageable limite);
}