package br.pucrs.tcc.bilheteria.compartilhado.infra.seguranca;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

// Quem está chamando, lido do token. É o que os controllers entregam aos serviços no lugar de um id vindo do corpo. 
public record UsuarioAutenticado(UUID id, Papel papel) {

    public static UsuarioAutenticado de(Jwt token) {
        return new UsuarioAutenticado(UUID.fromString(token.getSubject()), Papel.valueOf(token.getClaimAsString(ClaimsDoToken.PAPEL)));
    }

    public boolean ehEquipe() {
        return papel.ehEquipe();
    }
}