package br.pucrs.tcc.bilheteria;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import br.pucrs.tcc.bilheteria.Application;

class ModulithTest  {
    
    private final ApplicationModules modulos = ApplicationModules.of(Application.class);

    @Test
    void varificaFronteirasDosModulos() {
        modulos.verify();
    }

    @Test
    void geraDocumentacaoDosModulos() {
        new Documenter(modulos).writeDocumentation();
    }
}
