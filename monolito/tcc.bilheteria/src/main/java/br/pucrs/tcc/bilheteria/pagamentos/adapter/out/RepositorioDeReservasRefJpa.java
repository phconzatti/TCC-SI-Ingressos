package br.pucrs.tcc.bilheteria.pagamentos.adapter.out;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.pagamentos.application.RepositorioDeReservasRef;
import br.pucrs.tcc.bilheteria.pagamentos.domain.ReservaRef;

@Repository
class RepositorioDeReservasRefJpa implements RepositorioDeReservasRef {

    private final ReservaRefJpaRepository jpa;

    RepositorioDeReservasRefJpa(ReservaRefJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existe(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public void salvar(ReservaRef r) {
        ReservaRefJpa e = new ReservaRefJpa();
        e.id = r.id();
        e.usuarioId = r.usuarioId();
        e.eventoId = r.eventoId();
        e.valorCentavos = r.valor().centavos();
        e.expiraEm = r.expiraEm();
        e.expirada = r.expirada();
        jpa.save(e);
    }

    @Override
    public Optional<ReservaRef> buscar(UUID id) {
        return jpa.findById(id).map(e -> ReservaRef.reconstituir(e.id, e.usuarioId, e.eventoId, Dinheiro.deCentavos(e.valorCentavos), e.expiraEm, e.expirada));
    }
}