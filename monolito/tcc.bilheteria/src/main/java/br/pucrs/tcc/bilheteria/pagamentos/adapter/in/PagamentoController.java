package br.pucrs.tcc.bilheteria.pagamentos.adapter.in;

import br.pucrs.tcc.bilheteria.compartilhado.infra.seguranca.UsuarioAutenticado;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagamentoService;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagarReserva;
import br.pucrs.tcc.bilheteria.pagamentos.application.ResultadoDoPagamento;
import br.pucrs.tcc.bilheteria.pagamentos.domain.Pagamento;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/pagamentos")
class PagamentoController {

    private final PagamentoService service;

    PagamentoController(PagamentoService service) {
        this.service = service;
    }

    record PagarRequest(UUID reservaId, String tokenDePagamento) {}

    record PagamentoResponse(UUID id, UUID reservaId, long valorEmCentavos, String status, String motivo, Instant criadoEm) {
        static PagamentoResponse de(Pagamento p) {
            return new PagamentoResponse(p.id(), p.reservaId(), p.valor().centavos(), p.status().name(), p.motivo(), p.criadoEm());
        }
    }

    @PostMapping
    ResponseEntity<PagamentoResponse> pagar(@AuthenticationPrincipal Jwt jwt,
                                            @RequestHeader("Idempotency-Key") String chave,
                                            @RequestBody PagarRequest req) {
        UsuarioAutenticado usuario = UsuarioAutenticado.de(jwt);
        ResultadoDoPagamento resultado = service.pagar(new PagarReserva(usuario.id(), req.reservaId(), chave, req.tokenDePagamento()));
        PagamentoResponse corpo = PagamentoResponse.de(resultado.pagamento());
        if (resultado.novo()) {
            return ResponseEntity.created(URI.create("/pagamentos/" + corpo.id())).body(corpo);
        }
        return ResponseEntity.ok(corpo);
    }

    @GetMapping("/{id}")
    PagamentoResponse buscar(@PathVariable UUID id) {
        return PagamentoResponse.de(service.buscar(id));
    }
}