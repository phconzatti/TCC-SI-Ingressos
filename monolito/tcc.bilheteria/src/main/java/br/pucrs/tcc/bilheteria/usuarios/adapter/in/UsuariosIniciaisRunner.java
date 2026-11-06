package br.pucrs.tcc.bilheteria.usuarios.adapter.in;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import br.pucrs.tcc.bilheteria.usuarios.application.RegistrarUsuario;
import br.pucrs.tcc.bilheteria.usuarios.application.UsuarioService;

// Carga inicial para DESENVOLVIMENTO: garante um organizador e uma portaria. Desligue fora do ambiente local. 
@Component
@ConditionalOnProperty(name = "bilheteria.usuarios.iniciais.ativo", havingValue = "true")
class UsuariosIniciaisRunner implements ApplicationRunner {

    private final UsuarioService service;
    private final String organizadorEmail;
    private final String organizadorSenha;
    private final String portariaEmail;
    private final String portariaSenha;

    UsuariosIniciaisRunner(UsuarioService service,
                           @Value("${bilheteria.usuarios.iniciais.organizador-email}") String organizadorEmail,
                           @Value("${bilheteria.usuarios.iniciais.organizador-senha}") String organizadorSenha,
                           @Value("${bilheteria.usuarios.iniciais.portaria-email}") String portariaEmail,
                           @Value("${bilheteria.usuarios.iniciais.portaria-senha}") String portariaSenha) {
        this.service = service;
        this.organizadorEmail = organizadorEmail;
        this.organizadorSenha = organizadorSenha;
        this.portariaEmail = portariaEmail;
        this.portariaSenha = portariaSenha;
    }

    @Override
    public void run(ApplicationArguments args) {
        garantir("Organizador", organizadorEmail, organizadorSenha, Papel.ORGANIZADOR);
        garantir("Portaria", portariaEmail, portariaSenha, Papel.PORTARIA);
    }

    private void garantir(String nome, String email, String senha, Papel papel) {
        if (!service.existe(email)) {
            service.registrar(new RegistrarUsuario(nome, email, senha, papel));
        }
    }
}