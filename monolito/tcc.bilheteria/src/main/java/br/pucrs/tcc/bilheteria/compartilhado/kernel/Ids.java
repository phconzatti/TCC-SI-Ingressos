package br.pucrs.tcc.bilheteria.compartilhado.kernel;

// Ponto único de geração de id

import java.util.UUID;

public final class Ids {
    
    private Ids() {}

    public static UUID novo() {
        return UUID.randomUUID();
    }
}
