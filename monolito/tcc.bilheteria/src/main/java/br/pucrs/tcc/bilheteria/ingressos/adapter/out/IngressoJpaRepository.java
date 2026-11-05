package br.pucrs.tcc.bilheteria.ingressos.adapter.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface IngressoJpaRepository extends JpaRepository<IngressoJpa, UUID> {

    boolean existsByReservaId(UUID reservaId);

    List<IngressoJpa> findByReservaIdOrderByNumero(UUID reservaId);

    Optional<IngressoJpa> findByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IngressoJpa i where i.codigo = :codigo")
    Optional<IngressoJpa> buscarParaAtualizacao(@Param("codigo") String codigo);
}