package br.pucrs.tcc.bilheteria.eventos.adapter.out.persistence;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.eventos.application.RepositorioDeEventos;
import br.pucrs.tcc.bilheteria.eventos.domain.Categoria;
import br.pucrs.tcc.bilheteria.eventos.domain.Evento;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class RepositorioDeEventosJpa implements RepositorioDeEventos {
    
    private final EventoJpaRepository jpa;

    RepositorioDeEventosJpa(EventoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(Evento evento) {
        jpa.save(paraEntidade(evento));
    }

    @Override 
    public Optional<Evento> buscar(UUID id) {
        return jpa.findById(id).map(this::paraDominio);
    }

    @Override
    public List<Evento> listar() {
        return jpa.findAll().stream().map(this::paraDominio).toList();
    }

    private EventoJpa paraEntidade(Evento e) {
        EventoJpa entidade = new EventoJpa();
        entidade.id = e.id();
        entidade.nome = e.nome();
        entidade.local = e.local();
        entidade.dataDoEvento = e.dataDoEvento();
        entidade.capacidade = e.capacidade();
        entidade.status = e.status();
        for (Categoria c : e.categorias()) {
            CategoriaJpa cj = new CategoriaJpa();
            cj.id = c.id();
            cj.nome = c.nome();
            cj.precoCentavos = c.preco().centavos();
            cj.capacidade = c.capacidade();
            cj.inicioVendas = c.janelaDeVendas().inicio();
            cj.fimVendas = c.janelaDeVendas().fim();
            entidade.categorias.add(cj);
        }
        return entidade;
    }

    private Evento paraDominio(EventoJpa e) {
        List<Categoria> categorias = e.categorias.stream()
            .map(c -> new Categoria(c.id, c.nome, Dinheiro.deCentavos(c.precoCentavos), c.capacidade, Periodo.de(c.inicioVendas, c.fimVendas)))
            .toList(); 
        return Evento.reconstruir(e.id, e.nome, e.local, e.dataDoEvento, e.capacidade, e.status, categorias);
    }
}
