package br.pucrs.tcc.bilheteria.usuarios.application;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;

public record RegistrarUsuario(String nome, String email, String senha, Papel papel) {
}