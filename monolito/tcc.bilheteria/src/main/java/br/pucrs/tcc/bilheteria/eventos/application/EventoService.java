package br.pucrs.tcc.bilheteria.eventos.application;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.EventoPublicado;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.PublicadorDeEventos;
import br.pucrs.tcc.bilheteria.eventos.domain.Evento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class EventoService {
    
    private final RepositorioDeEventos repositorio;
    private final PublicadorDeEventos publicador;
    private final Clock relogio;

    public EventoService(RepositorioDeEventos repositorio, PublicadorDeEventos publicador, Clock relogio) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public Evento criar(CriarEvento cmd) {
        Evento evento = Evento.criar(cmd.nome(), cmd.local(), cmd.dataDoEvento(), cmd.capacidade(), relogio.instant());
        repositorio.salvar(evento);
        return evento;
    }

    @Transactional 
    public Evento adicionarCategoria(UUID eventoId, AdicionarCategoria cmd) {
        Evento evento = buscarOuFalhar(eventoId);
        evento.adicionarCategoria(cmd.nome(), Dinheiro.deCentavos(cmd.precoEmCentavos()), cmd.capacidade(), Periodo.de(cmd.inicioDasVendas(), cmd.fimDasVendas()));
        repositorio.salvar(evento);
        return evento;
    }

    @Transactional
    public Evento publicar(UUID eventoId) {
        Evento evento = buscarOuFalhar(eventoId);
        evento.publicar();
        repositorio.salvar(evento);
        publicador.publicar(new EventoPublicado(Ids.novo(), relogio.instant(), evento.id(), evento.nome(), evento.dataDoEvento(), evento.categorias().stream()
        .map(c -> new EventoPublicado.CategoriaPublicada(
            c.id(), c.nome(), c.preco().centavos(), c.capacidade(), 
            c.janelaDeVendas().inicio(), c.janelaDeVendas().fim())).toList()));
    return evento;
    }

    @Transactional(readOnly = true)
    public Evento buscar(UUID id) {
        return buscarOuFalhar(id);
    }

    @Transactional(readOnly = true)
    public List<Evento> listar() {
        return repositorio.listar();
    }

    private Evento buscarOuFalhar(UUID id) {
        return repositorio.buscar(id).orElseThrow(() -> new NaoEncontradoException("evento.nao-encontrado", "Evento não encontrado: " + id));
    }
}    
