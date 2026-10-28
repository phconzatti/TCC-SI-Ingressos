package br.pucrs.tcc.bilheteria.reservas.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.reservas.domain.Disponibilidade;

public interface RepositorioDeDisponibilidades {
    boolean existe(UUID id);
    void salvar(Disponibilidade disponibilidade);
    Optional<Disponibilidade> buscarParaAtualizacao(UUID id);
    List<Disponibilidade> listarPorEvento(UUID eventoId);
}
