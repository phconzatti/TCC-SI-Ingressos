package br.pucrs.tcc.bilheteria.pagamentos.application;

import java.util.Optional;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.pagamentos.domain.ReservaRef;

public interface RepositorioDeReservasRef {
    boolean existe(UUID id);
    void salvar(ReservaRef reserva);
    Optional<ReservaRef> buscar(UUID id);
}