package br.pucrs.tcc.bilheteria.pagamentos.adapter.out;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReservaRefJpaRepository extends JpaRepository<ReservaRefJpa, UUID> {
}