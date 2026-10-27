package br.pucrs.tcc.bilheteria.eventos.adapter.in.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.pucrs.tcc.bilheteria.eventos.application.AdicionarCategoria;
import br.pucrs.tcc.bilheteria.eventos.application.CriarEvento;
import br.pucrs.tcc.bilheteria.eventos.application.EventoService;
import br.pucrs.tcc.bilheteria.eventos.domain.Categoria;
import br.pucrs.tcc.bilheteria.eventos.domain.Evento;

@RestController
@RequestMapping("/eventos")
class EventoController {

    private final EventoService service;

    EventoController(EventoService service) {
        this.service = service;
    }

    record CriarEventoRequest(String nome, String local, Instant dataDoEvento, int capacidade) {}

    record CategoriaRequest(String nome, long precoEmCentavos, int capacidade, Instant inicioDasVendas, Instant fimDasVendas) {}

    record CategoriaResponse(UUID id, String nome, long precoEmCentavos, int capacidade,Instant inicioDasVendas, Instant fimDasVendas) {
        static CategoriaResponse de(Categoria c) {
            return new CategoriaResponse(c.id(), c.nome(), c.preco().centavos(), c.capacidade(), c.janelaDeVendas().inicio(), c.janelaDeVendas().fim());
        }
    }

    record EventoResponse(UUID id, String nome, String local, Instant dataDoEvento, int capacidade, String status, List<CategoriaResponse> categorias) {
        static EventoResponse de(Evento e) {
            return new EventoResponse(e.id(), e.nome(), e.local(), e.dataDoEvento(), e.capacidade(), e.status().name(), e.categorias().stream().map(CategoriaResponse::de).toList());
        }
    }

    @PostMapping
    ResponseEntity<EventoResponse> criar(@RequestBody CriarEventoRequest req) {
        Evento evento = service.criar(new CriarEvento(req.nome(), req.local(), req.dataDoEvento(), req.capacidade()));
        return ResponseEntity.created(URI.create("/eventos/" + evento.id())).body(EventoResponse.de(evento));
    }

    @GetMapping
    List<EventoResponse> listar() {
        return service.listar().stream().map(EventoResponse::de).toList();
    }

    @GetMapping("/{id}")
    EventoResponse buscar(@PathVariable UUID id) {
        return EventoResponse.de(service.buscar(id));
    }

    @PostMapping("/{id}/categorias")
    ResponseEntity<EventoResponse> adicionarCategoria(@PathVariable UUID id, @RequestBody CategoriaRequest req) {
        Evento evento = service.adicionarCategoria(id, new AdicionarCategoria(req.nome(), req.precoEmCentavos(), req.capacidade(), req.inicioDasVendas(), req.fimDasVendas()));
        return ResponseEntity.status(201).body(EventoResponse.de(evento));
    }

    @PostMapping("/{id}/publicacao")
    EventoResponse publicar(@PathVariable UUID id) {
        return EventoResponse.de(service.publicar(id));
    }
}