package br.pucrs.tcc.bilheteria.ingressos.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.ingressos.domain.Ingresso;

public interface RepositorioDeIngressos {

    void salvarTodos(List<Ingresso> ingressos);
    void salvar(Ingresso ingresso);
    boolean existePorReserva(UUID reservaId);
    List<Ingresso> listarPorReserva(UUID reservaId);
    Optional<Ingresso> buscarPorCodigo(String codigo);
    Optional<Ingresso> buscarPorCodigoParaAtualizacao(String codigo);
}