package br.pucrs.tcc.bilheteria;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


@AnalyzeClasses(packages = "br.pucrs.tcc.bilheteria", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {
    
    @ArchTest
    static final ArchRule dominioSemFrameworks = noClasses()
        .that().resideInAnyPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..");

    @ArchTest
    static final ArchRule aplicacaoSemWebNemPersistencia = noClasses()
        .that().resideInAnyPackage("..application..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "org.springframework.data..", "jakarta.persistence.."); 

    @ArchTest
    static final ArchRule nucleoNaoDependeDeAdaptadores = noClasses()
        .that().resideInAnyPackage("..domain..", "..application..")
        .should().dependOnClassesThat().resideInAnyPackage("..adapter..");

    @ArchTest
    static final ArchRule kernelEContratosSemFrameworks = noClasses()
        .that().resideInAnyPackage("..compartilhado.kernel..", "..compartilhado.contratos..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..");
}
