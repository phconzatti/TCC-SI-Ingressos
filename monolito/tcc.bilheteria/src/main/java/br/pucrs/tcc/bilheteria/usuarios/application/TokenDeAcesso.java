package br.pucrs.tcc.bilheteria.usuarios.application;

public record TokenDeAcesso(String valor, long expiraEmSegundos) {
}