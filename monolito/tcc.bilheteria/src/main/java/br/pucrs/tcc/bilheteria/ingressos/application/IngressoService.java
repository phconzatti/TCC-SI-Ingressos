package br.pucrs.tcc.bilheteria.ingressos.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaConfirmada;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;
import br.pucrs.tcc.bilheteria.ingressos.domain.Ingresso;

@Service
public class IngressoService {

    private final RepositorioDeIngressos ingressos;
    private final GeradorDeCodigo gerador;
    private final Clock relogio;

    public IngressoService(RepositorioDeIngressos ingressos, GeradorDeCodigo gerador, Clock relogio) {
        this.ingressos = ingressos;
        this.gerador = gerador;
        this.relogio = relogio;
    }

    @Transactional
    public void emitirIngressos(ReservaConfirmada evento) {
        if (ingressos.existePorReserva(evento.reservaId())) {
            return;
        }
        List<Ingresso> emitidos = new ArrayList<>();
        for (int numero = 1; numero <= evento.quantidade(); numero++) {
            emitidos.add(Ingresso.emitir(evento.reservaId(), numero, evento.eventoId(), evento.categoriaId(), evento.usuarioId(), gerador.novo(), relogio.instant()));
        }
        ingressos.salvarTodos(emitidos);
    }

    @Transactional
    public Ingresso realizarCheckIn(String codigo) {
        Ingresso ingresso = ingressos.buscarPorCodigoParaAtualizacao(codigo).orElseThrow(() -> new NaoEncontradoException("ingresso.nao-encontrado", "Ingresso não encontrado"));
        ingresso.realizarCheckIn(relogio.instant());
        ingressos.salvar(ingresso);
        return ingresso;
    }

    @Transactional(readOnly = true)
    public Ingresso buscarPorCodigo(String codigo) {
        return ingressos.buscarPorCodigo(codigo).orElseThrow(() -> new NaoEncontradoException("ingresso.nao-encontrado", "Ingresso não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Ingresso> listarPorReserva(UUID reservaId) {
        return ingressos.listarPorReserva(reservaId);
    }

    @Transactional(readOnly = true)
    public List<Ingresso> listarDoUsuario(UUID reservaId, UUID usuarioId) {
        List<Ingresso> todos = ingressos.listarPorReserva(reservaId);
        if (todos.stream().anyMatch(i -> !i.usuarioId().equals(usuarioId))) {
            throw new AcessoNegadoException("ingresso.de-outro-usuario", "Estes ingressos pertencem a outro usuário");
        }
        return todos;
    }

    @Transactional(readOnly = true)
    public Ingresso buscarDoUsuario(String codigo, UUID usuarioId) {
        Ingresso ingresso = buscarPorCodigo(codigo);
        if (!ingresso.usuarioId().equals(usuarioId)) {
            throw new AcessoNegadoException("ingresso.de-outro-usuario", "Este ingresso pertence a outro usuário");
        }
        return ingresso;
    }
}