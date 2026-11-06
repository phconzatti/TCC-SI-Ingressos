package br.pucrs.tcc.bilheteria.usuarios.adapter.out;

import br.pucrs.tcc.bilheteria.usuarios.application.CodificadorDeSenha;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class CodificadorBcrypt implements CodificadorDeSenha {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String codificar(String senha) {
        return bcrypt.encode(senha);
    }

    @Override
    public boolean confere(String senha, String hash) {
        return bcrypt.matches(senha, hash);
    }
}