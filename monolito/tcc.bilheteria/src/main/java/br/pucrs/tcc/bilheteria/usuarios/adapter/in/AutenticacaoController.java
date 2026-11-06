package br.pucrs.tcc.bilheteria.usuarios.adapter.in;

import br.pucrs.tcc.bilheteria.compartilhado.infra.seguranca.UsuarioAutenticado;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import br.pucrs.tcc.bilheteria.usuarios.application.RegistrarUsuario;
import br.pucrs.tcc.bilheteria.usuarios.application.TokenDeAcesso;
import br.pucrs.tcc.bilheteria.usuarios.application.UsuarioService;
import br.pucrs.tcc.bilheteria.usuarios.domain.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
class AutenticacaoController {

    private final UsuarioService service;

    AutenticacaoController(UsuarioService service) {
        this.service = service;
    }

    record RegistrarRequest(String nome, String email, String senha) {}

    record LoginRequest(String email, String senha) {}

    record UsuarioResponse(UUID id, String nome, String email, String papel, Instant criadoEm) {
        static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.id(), u.nome(), u.email(), u.papel().name(), u.criadoEm());
        }
    }

    record TokenResponse(String accessToken, String tokenType, long expiresIn) {
        static TokenResponse de(TokenDeAcesso t) {
            return new TokenResponse(t.valor(), "Bearer", t.expiraEmSegundos());
        }
    }

    @PostMapping("/usuarios")
    ResponseEntity<UsuarioResponse> registrar(@RequestBody RegistrarRequest req) {
        Usuario usuario = service.registrar(
                new RegistrarUsuario(req.nome(), req.email(), req.senha(), Papel.CLIENTE));
        return ResponseEntity.created(URI.create("/usuarios/" + usuario.id())).body(UsuarioResponse.de(usuario));
    }

    @PostMapping("/auth/login")
    TokenResponse login(@RequestBody LoginRequest req) {
        return TokenResponse.de(service.autenticar(req.email(), req.senha()));
    }

    @GetMapping("/usuarios/eu")
    UsuarioResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.de(service.buscar(UsuarioAutenticado.de(jwt).id()));
    }
}