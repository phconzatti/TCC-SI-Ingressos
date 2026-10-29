package br.pucrs.tcc.bilheteria.reservas.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.reservas.application.RepositorioDeReservas;
import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;
import br.pucrs.tcc.bilheteria.reservas.domain.StatusReserva;

@Repository
class RepositorioDeReservasJpa implements RepositorioDeReservas {

    private final ReservaJpaRepository jpa;

    RepositorioDeReservasJpa(ReservaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(Reserva r) {
        ReservaJpa e = new ReservaJpa();
        e.id = r.id();
        e.usuarioId = r.usuarioId();
        e.eventoId = r.eventoId();
        e.categoriaId = r.categoriaId();
        e.quantidade = r.quantidade();
        e.valorTotalCentavos = r.valorTotal().centavos();
        e.status = r.status();
        e.criadaEm = r.criadaEm();
        e.expiraEm = r.expiraEm();
        jpa.save(e);
    }

    @Override
    public Optional<Reserva> buscar(UUID id) {
        return jpa.findById(id).map(this::paraDominio);
    }

    @Override
    public Optional<Reserva> buscarParaAtualizacao(UUID id) {
        return jpa.buscarParaAtualizacao(id).map(this::paraDominio);
    }

    @Override
    public List<UUID> idsVencidas(Instant agora, int limite) {
        return jpa.idsVencidas(StatusReserva.AGUARDANDO_PAGAMENTO, agora, PageRequest.ofSize(limite));
    }

    private Reserva paraDominio(ReservaJpa e) {
        return Reserva.reconstituir(e.id, e.usuarioId, e.eventoId, e.categoriaId, e.quantidade, Dinheiro.deCentavos(e.valorTotalCentavos), e.criadaEm, e.expiraEm, e.status);
    }
}