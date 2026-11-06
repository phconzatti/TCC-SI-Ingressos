package br.pucrs.tcc.bilheteria.usuarios.application;

import br.pucrs.tcc.bilheteria.usuarios.domain.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeUsuarios {

    void salvar(Usuario usuario);
    Optional<Usuario> buscar(UUID id);
    Optional<Usuario> buscarPorEmail(String emailNormalizado);
    boolean existePorEmail(String emailNormalizado);
}