package br.pucrs.tcc.bilheteria.usuarios.adapter.out;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario", schema = "usuarios")
class UsuarioJpa {

    @Id
    UUID id;

    String nome;

    String email;

    @Column(name = "senha_hash")
    String senhaHash;

    @Enumerated(EnumType.STRING)
    Papel papel;

    @Column(name = "criado_em")
    Instant criadoEm;

    protected UsuarioJpa() {
    }
}