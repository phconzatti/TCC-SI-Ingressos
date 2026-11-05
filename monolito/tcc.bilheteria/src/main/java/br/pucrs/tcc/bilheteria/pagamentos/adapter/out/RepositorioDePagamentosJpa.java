package br.pucrs.tcc.bilheteria.pagamentos.adapter.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.pagamentos.application.RepositorioDePagamentos;
import br.pucrs.tcc.bilheteria.pagamentos.domain.Pagamento;
import br.pucrs.tcc.bilheteria.pagamentos.domain.StatusPagamento;

@Repository
class RepositorioDePagamentosJpa implements RepositorioDePagamentos {

    private static final Set<StatusPagamento> ATIVOS = Set.of(StatusPagamento.INICIADO, StatusPagamento.APROVADO);

    private final PagamentoJpaRepository jpa;

    RepositorioDePagamentosJpa(PagamentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(Pagamento p) {
        PagamentoJpa e = new PagamentoJpa();
        e.id = p.id();
        e.reservaId = p.reservaId();
        e.chaveDeIdempotencia = p.chaveDeIdempotencia();
        e.valorCentavos = p.valor().centavos();
        e.status = p.status();
        e.motivo = p.motivo();
        e.codigoGateway = p.codigoNoGateway();
        e.criadoEm = p.criadoEm();
        try {
            jpa.saveAndFlush(e);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("pagamento.conflito", "Já existe pagamento ativo para esta reserva, ou a chave de idempotência já foi usada");
        }
    }

    @Override
    public Optional<Pagamento> buscar(UUID id) {
        return jpa.findById(id).map(this::paraDominio);
    }

    @Override
    public Optional<Pagamento> buscarPorChave(String chave) {
        return jpa.findByChaveDeIdempotencia(chave).map(this::paraDominio);
    }

    @Override
    public boolean existeAtivoParaReserva(UUID reservaId) {
        return jpa.existsByReservaIdAndStatusIn(reservaId, ATIVOS);
    }

    @Override
    public List<Pagamento> listarPorReserva(UUID reservaId) {
        return jpa.findByReservaId(reservaId).stream().map(this::paraDominio).toList();
    }

    private Pagamento paraDominio(PagamentoJpa e) {
        return Pagamento.reconstituir(e.id, e.reservaId, e.chaveDeIdempotencia, Dinheiro.deCentavos(e.valorCentavos), e.criadoEm, e.status, e.motivo, e.codigoGateway);
    }
}