package br.pucrs.tcc.bilheteria.reservas.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.reservas.application.RepositorioDeDisponibilidades;
import br.pucrs.tcc.bilheteria.reservas.domain.Disponibilidade;

@Repository
class RepositorioDeDisponibilidadesJpa implements RepositorioDeDisponibilidades {

    private final DisponibilidadeJpaRepository jpa;

    RepositorioDeDisponibilidadesJpa(DisponibilidadeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existe(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public void salvar(Disponibilidade d) {
        DisponibilidadeJpa e = new DisponibilidadeJpa();
        e.id = d.id();
        e.eventoId = d.eventoId();
        e.nomeCategoria = d.nomeCategoria();
        e.precoCentavos = d.preco().centavos();
        e.capacidade = d.capacidade();
        e.reservados = d.reservados();
        e.inicioVendas = d.janelaDeVendas().inicio();
        e.fimVendas = d.janelaDeVendas().fim();
        jpa.save(e);
    }

    @Override
    public Optional<Disponibilidade> buscarParaAtualizacao(UUID id) {
        return jpa.buscarParaAtualizacao(id).map(this::paraDominio);
    }

    @Override
    public List<Disponibilidade> listarPorEvento(UUID eventoId) {
        return jpa.findByEventoId(eventoId).stream().map(this::paraDominio).toList();
    }

    private Disponibilidade paraDominio(DisponibilidadeJpa e) {
        return Disponibilidade.reconstituir(e.id, e.eventoId, e.nomeCategoria, Dinheiro.deCentavos(e.precoCentavos), e.capacidade, Periodo.de(e.inicioVendas, e.fimVendas), e.reservados);
    }
}