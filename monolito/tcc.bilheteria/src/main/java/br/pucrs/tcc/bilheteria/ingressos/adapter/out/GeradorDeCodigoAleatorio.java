package br.pucrs.tcc.bilheteria.ingressos.adapter.out;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.ingressos.application.GeradorDeCodigo;

// 128 bits aleatórios em Base64 URL-safe (22 caracteres)
@Component
class GeradorDeCodigoAleatorio implements GeradorDeCodigo {

    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public String novo() {
        byte[] bytes = new byte[16];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}