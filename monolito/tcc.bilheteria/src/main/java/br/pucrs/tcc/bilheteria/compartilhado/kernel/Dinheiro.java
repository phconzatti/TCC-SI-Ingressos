package br.pucrs.tcc.bilheteria.compartilhado.kernel;

// Valor monetário em reais, armazenado em centavos. Nunca é negativo.

import java.util.Locale;

public record Dinheiro(long centavos) implements Comparable<Dinheiro> {

    public static final Dinheiro ZERO = new Dinheiro(0);

    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    
    public Dinheiro {
        if (centavos < 0) {
            throw new RegraDeNegocioException("dinheiro.negativo", "O valor monetário não pode ser negativo");
        }
    }

    public static Dinheiro deCentavos(long centavos) {
        return new Dinheiro(centavos);
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(Math.addExact(centavos, outro.centavos));
    }

    // Lança RegraDeNegocioException se o resultado for negativo
    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(centavos - outro.centavos);
    }

    public Dinheiro multiplicar(int quantidade) {
        if (quantidade < 0) {
            throw new RegraDeNegocioException("dinheiro.quantidade-negativa", "A quantidade não pode ser negativa");
        }
        return new Dinheiro(Math.multiplyExact(centavos, (long) quantidade));
    }

    public boolean maiorQue(Dinheiro outro) {
        return centavos > outro.centavos;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return Long.compare(centavos, outro.centavos);
    }

    @Override
    public String toString() {
        return String.format(PT_BR, "R$ %,d,%02d", centavos / 100, centavos % 100);
    }
}
