package br.pucrs.tcc.bilheteria.usuarios.application;

import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;

// Porta de saída: emite o token assinado que carrega a identidade (id e papel) do usuário. 
public interface EmissorDeToken {
    TokenDeAcesso emitir(UUID usuarioId, Papel papel);
}