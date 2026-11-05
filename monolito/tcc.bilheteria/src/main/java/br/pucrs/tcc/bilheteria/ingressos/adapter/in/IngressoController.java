package br.pucrs.tcc.bilheteria.ingressos.adapter.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.pucrs.tcc.bilheteria.ingressos.application.IngressoService;
import br.pucrs.tcc.bilheteria.ingressos.domain.Ingresso;

@RestController
@RequestMapping("/ingressos")
class IngressoController {

    private final IngressoService service;

    IngressoController(IngressoService service) {
        this.service = service;
    }

    record IngressoResponse(UUID id, UUID reservaId, int numero, UUID eventoId, UUID categoriaId, UUID usuarioId, String codigo, String status, Instant emitidoEm, Instant utilizadoEm) {
        static IngressoResponse de(Ingresso i) {
            return new IngressoResponse(i.id(), i.reservaId(), i.numero(), i.eventoId(), i.categoriaId(), i.usuarioId(), i.codigo(), i.status().name(), i.emitidoEm(), i.utilizadoEm());
        }
    }

    @GetMapping
    List<IngressoResponse> listarPorReserva(@RequestParam UUID reservaId) {
        return service.listarPorReserva(reservaId).stream().map(IngressoResponse::de).toList();
    }

    @GetMapping("/{codigo}")
    IngressoResponse buscar(@PathVariable String codigo) {
        return IngressoResponse.de(service.buscarPorCodigo(codigo));
    }

    @PostMapping("/{codigo}/check-in")
    IngressoResponse checkIn(@PathVariable String codigo) {
        return IngressoResponse.de(service.realizarCheckIn(codigo));
    }
}