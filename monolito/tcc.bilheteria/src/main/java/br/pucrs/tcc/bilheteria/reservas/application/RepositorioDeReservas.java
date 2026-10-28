package br.pucrs.tcc.bilheteria.reservas.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;

public interface RepositorioDeReservas {
    void salvar(Reserva reserva);
    Optional<Reserva> buscar(UUID id);
    Optional<Reserva> buscarParaAtualizacao(UUID id);
    List<UUID> idsVencidas(Instant agora, int limite);
}
