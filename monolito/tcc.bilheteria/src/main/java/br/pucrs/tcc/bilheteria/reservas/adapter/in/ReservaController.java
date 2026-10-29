package br.pucrs.tcc.bilheteria.reservas.adapter.in;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.pucrs.tcc.bilheteria.reservas.application.ReservaService;
import br.pucrs.tcc.bilheteria.reservas.application.ReservarIngressos;
import br.pucrs.tcc.bilheteria.reservas.domain.Disponibilidade;
import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;

@RestController
class ReservaController {

    private final ReservaService service;

    ReservaController(ReservaService service) {
        this.service = service;
    }

    record ReservarRequest(UUID usuarioId, UUID categoriaId, int quantidade) {}

    record ReservaResponse(UUID id, UUID usuarioId, UUID eventoId, UUID categoriaId, int quantidade, long valorTotalEmCentavos, String status, Instant criadaEm, Instant expiraEm) {
        static ReservaResponse de(Reserva r) {
            return new ReservaResponse(r.id(), r.usuarioId(), r.eventoId(), r.categoriaId(), r.quantidade(), r.valorTotal().centavos(), r.status().name(), r.criadaEm(), r.expiraEm());
        }
    }

    record DisponibilidadeResponse(UUID categoriaId, UUID eventoId, String nomeCategoria, long precoEmCentavos, int capacidade, int reservados, int disponiveis) {
        static DisponibilidadeResponse de(Disponibilidade d) {
            return new DisponibilidadeResponse(d.id(), d.eventoId(), d.nomeCategoria(), d.preco().centavos(), d.capacidade(), d.reservados(), d.disponiveis());
        }
    }

    @PostMapping("/reservas")
    ResponseEntity<ReservaResponse> reservar(@RequestBody ReservarRequest req) {
        Reserva reserva = service.reservar(new ReservarIngressos(req.usuarioId(), req.categoriaId(), req.quantidade()));
        return ResponseEntity.created(URI.create("/reservas/" + reserva.id())).body(ReservaResponse.de(reserva));
    }

    @GetMapping("/reservas/{id}")
    ReservaResponse buscar(@PathVariable UUID id) {
        return ReservaResponse.de(service.buscar(id));
    }

    @GetMapping("/disponibilidades")
    List<DisponibilidadeResponse> disponibilidades(@RequestParam UUID eventoId) {
        return service.disponibilidadesDoEvento(eventoId).stream().map(DisponibilidadeResponse::de).toList();
    }
}