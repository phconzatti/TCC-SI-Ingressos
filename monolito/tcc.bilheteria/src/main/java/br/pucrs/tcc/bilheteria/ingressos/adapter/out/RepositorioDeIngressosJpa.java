package br.pucrs.tcc.bilheteria.ingressos.adapter.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.ingressos.application.RepositorioDeIngressos;
import br.pucrs.tcc.bilheteria.ingressos.domain.Ingresso;

@Repository
class RepositorioDeIngressosJpa implements RepositorioDeIngressos {

    private final IngressoJpaRepository jpa;

    RepositorioDeIngressosJpa(IngressoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvarTodos(List<Ingresso> ingressos) {
        try {
            jpa.saveAllAndFlush(ingressos.stream().map(this::paraEntidade).toList());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitoException("ingresso.conflito", "Os ingressos desta reserva já foram emitidos, ou o código gerado já existe");
        }
    }

    @Override
    public void salvar(Ingresso ingresso) {
        jpa.save(paraEntidade(ingresso));
    }

    @Override
    public boolean existePorReserva(UUID reservaId) {
        return jpa.existsByReservaId(reservaId);
    }

    @Override
    public List<Ingresso> listarPorReserva(UUID reservaId) {
        return jpa.findByReservaIdOrderByNumero(reservaId).stream().map(this::paraDominio).toList();
    }

    @Override
    public Optional<Ingresso> buscarPorCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(this::paraDominio);
    }

    @Override
    public Optional<Ingresso> buscarPorCodigoParaAtualizacao(String codigo) {
        return jpa.buscarParaAtualizacao(codigo).map(this::paraDominio);
    }

    private IngressoJpa paraEntidade(Ingresso i) {
        IngressoJpa e = new IngressoJpa();
        e.id = i.id();
        e.reservaId = i.reservaId();
        e.numero = i.numero();
        e.eventoId = i.eventoId();
        e.categoriaId = i.categoriaId();
        e.usuarioId = i.usuarioId();
        e.codigo = i.codigo();
        e.status = i.status();
        e.emitidoEm = i.emitidoEm();
        e.utilizadoEm = i.utilizadoEm();
        return e;
    }

    private Ingresso paraDominio(IngressoJpa e) {
        return Ingresso.reconstituir(e.id, e.reservaId, e.numero, e.eventoId, e.categoriaId, e.usuarioId, e.codigo, e.emitidoEm, e.status, e.utilizadoEm);
    }
}