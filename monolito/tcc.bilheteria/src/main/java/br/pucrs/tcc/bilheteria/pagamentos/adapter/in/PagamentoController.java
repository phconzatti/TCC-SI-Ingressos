package br.pucrs.tcc.bilheteria.pagamentos.adapter.in;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.pucrs.tcc.bilheteria.pagamentos.application.PagamentoService;
import br.pucrs.tcc.bilheteria.pagamentos.application.PagarReserva;
import br.pucrs.tcc.bilheteria.pagamentos.application.ResultadoDoPagamento;
import br.pucrs.tcc.bilheteria.pagamentos.domain.Pagamento;

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
    ResponseEntity<PagamentoResponse> pagar(@RequestHeader("Idempotency-Key") String chave, @RequestBody PagarRequest req) {
        ResultadoDoPagamento resultado = service.pagar(new PagarReserva(req.reservaId(), chave, req.tokenDePagamento()));
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