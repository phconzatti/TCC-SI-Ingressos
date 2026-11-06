package br.pucrs.tcc.bilheteria.usuarios.application;

// Porta de saída: transforma a senha em hash e confere uma senha contra um hash
public interface CodificadorDeSenha {
    String codificar(String senha);
    boolean confere(String senha, String hash);
}