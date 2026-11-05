package br.pucrs.tcc.bilheteria.pagamentos.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.pagamentos.domain.Pagamento;

public interface RepositorioDePagamentos {
    void salvar(Pagamento pagamento);
    Optional<Pagamento> buscar(UUID id);
    Optional<Pagamento> buscarPorChave(String chaveDeIdempotencia);
    boolean existeAtivoParaReserva(UUID reservaId);
    List<Pagamento> listarPorReserva(UUID reservaId);
}