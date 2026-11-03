package br.pucrs.tcc.bilheteria.pagamentos.adapter.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.pucrs.tcc.bilheteria.pagamentos.domain.StatusPagamento;

interface PagamentoJpaRepository extends JpaRepository<PagamentoJpa, UUID> {

    Optional<PagamentoJpa> findByChaveDeIdempotencia(String chaveDeIdempotencia);

    boolean existsByReservaIdAndStatusIn(UUID reservaId, Collection<StatusPagamento> status);

    List<PagamentoJpa> findByReservaId(UUID reservaId);
}