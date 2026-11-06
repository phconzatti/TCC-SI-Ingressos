package br.pucrs.tcc.bilheteria.usuarios.adapter.out;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.usuarios.application.RepositorioDeUsuarios;
import br.pucrs.tcc.bilheteria.usuarios.domain.Usuario;

@Repository
class RepositorioDeUsuariosJpa implements RepositorioDeUsuarios {

    private final UsuarioJpaRepository jpa;

    RepositorioDeUsuariosJpa(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(Usuario u) {
        UsuarioJpa e = new UsuarioJpa();
        e.id = u.id();
        e.nome = u.nome();
        e.email = u.email();
        e.senhaHash = u.senhaHash();
        e.papel = u.papel();
        e.criadoEm = u.criadoEm();
        try {
            jpa.saveAndFlush(e);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("usuario.email-ja-cadastrado", "Este e-mail já está cadastrado");
        }
    }

    @Override
    public Optional<Usuario> buscar(UUID id) {
        return jpa.findById(id).map(this::paraDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String emailNormalizado) {
        return jpa.findByEmail(emailNormalizado).map(this::paraDominio);
    }

    @Override
    public boolean existePorEmail(String emailNormalizado) {
        return jpa.existsByEmail(emailNormalizado);
    }

    private Usuario paraDominio(UsuarioJpa e) {
        return Usuario.reconstituir(e.id, e.nome, e.email, e.senhaHash, e.papel, e.criadoEm);
    }
}