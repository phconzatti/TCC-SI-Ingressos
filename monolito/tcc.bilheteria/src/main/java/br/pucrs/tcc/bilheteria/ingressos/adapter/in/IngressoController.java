package br.pucrs.tcc.bilheteria.ingressos.adapter.in;

import br.pucrs.tcc.bilheteria.compartilhado.infra.seguranca.UsuarioAutenticado;
import br.pucrs.tcc.bilheteria.ingressos.application.IngressoService;
import br.pucrs.tcc.bilheteria.ingressos.domain.Ingresso;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ingressos")
class IngressoController {

    private final IngressoService service;

    IngressoController(IngressoService service) {
        this.service = service;
    }

    record IngressoResponse(UUID id, UUID reservaId, int numero, UUID eventoId, UUID categoriaId,
                            UUID usuarioId, String codigo, String status, Instant emitidoEm,
                            Instant utilizadoEm) {
        static IngressoResponse de(Ingresso i) {
            return new IngressoResponse(i.id(), i.reservaId(), i.numero(), i.eventoId(), i.categoriaId(),
                    i.usuarioId(), i.codigo(), i.status().name(), i.emitidoEm(), i.utilizadoEm());
        }
    }

    @GetMapping
    List<IngressoResponse> listarPorReserva(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID reservaId) {
        UsuarioAutenticado usuario = UsuarioAutenticado.de(jwt);
        List<Ingresso> ingressos = usuario.ehEquipe()
                ? service.listarPorReserva(reservaId)
                : service.listarDoUsuario(reservaId, usuario.id());
        return ingressos.stream().map(IngressoResponse::de).toList();
    }

    @GetMapping("/{codigo}")
    IngressoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable String codigo) {
        UsuarioAutenticado usuario = UsuarioAutenticado.de(jwt);
        return IngressoResponse.de(usuario.ehEquipe()
                ? service.buscarPorCodigo(codigo)
                : service.buscarDoUsuario(codigo, usuario.id()));
    }

    @PostMapping("/{codigo}/check-in")
    IngressoResponse checkIn(@PathVariable String codigo) {
        return IngressoResponse.de(service.realizarCheckIn(codigo));
    }
}