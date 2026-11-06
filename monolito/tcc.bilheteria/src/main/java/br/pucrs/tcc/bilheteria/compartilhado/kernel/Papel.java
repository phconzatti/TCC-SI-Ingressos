package br.pucrs.tcc.bilheteria.compartilhado.kernel;

public enum Papel {
    CLIENTE,
    ORGANIZADOR,
    PORTARIA;

    public boolean ehEquipe() {
        return this == ORGANIZADOR || this == PORTARIA;
    }
}
