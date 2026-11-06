package br.pucrs.tcc.bilheteria.usuarios.adapter.out;

import br.pucrs.tcc.bilheteria.compartilhado.infra.seguranca.ClaimsDoToken;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import br.pucrs.tcc.bilheteria.usuarios.application.EmissorDeToken;
import br.pucrs.tcc.bilheteria.usuarios.application.TokenDeAcesso;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
class EmissorDeTokenJwt implements EmissorDeToken {

    private final JwtEncoder encoder;
    private final Clock relogio;
    private final Duration validade;

    EmissorDeTokenJwt(JwtEncoder encoder, Clock relogio,
                      @Value("${bilheteria.seguranca.jwt.validade:PT1H}") Duration validade) {
        this.encoder = encoder;
        this.relogio = relogio;
        this.validade = validade;
    }

    @Override
    public TokenDeAcesso emitir(UUID usuarioId, Papel papel) {
        Instant agora = relogio.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ClaimsDoToken.EMISSOR)
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .claim(ClaimsDoToken.PAPEL, papel.name())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenDeAcesso(valor, validade.toSeconds());
    }
}