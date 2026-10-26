package br.pucrs.tcc.bilheteria.eventos.application;

import br.pucrs.tcc.bilheteria.eventos.domain.Evento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDeEventos {
    void salvar(Evento evento);
    Optional<Evento> buscar(UUID id);
    List<Evento> listar();
}
