package br.pucrs.tcc.bilheteria.usuarios.adapter.out;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioJpaRepository extends JpaRepository<UsuarioJpa, UUID> {

    Optional<UsuarioJpa> findByEmail(String email);

    boolean existsByEmail(String email);
}