
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

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
