package br.pucrs.tcc.bilheteria.reservas.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.EventoPublicado;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaConfirmada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaCriada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaExpirada;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.AcessoNegadoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.PublicadorDeEventos;
import br.pucrs.tcc.bilheteria.reservas.domain.Disponibilidade;
import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;
import br.pucrs.tcc.bilheteria.reservas.domain.StatusReserva;

@Service
public class ReservaService {
    private static final int LOTE_DE_EXPIRACAO = 100;

    private final RepositorioDeDisponibilidades disponibilidades;
    private final RepositorioDeReservas reservas;
    private final PublicadorDeEventos publicador;
    private final Clock relogio;
    private final Duration validade;

    public ReservaService(RepositorioDeDisponibilidades disponibilidades, RepositorioDeReservas reservas, PublicadorDeEventos publicador, Clock relogio, @Value("${bilheteria.reservas.validade:PT15M}") Duration validade) {
        this.disponibilidades = disponibilidades;
        this.reservas = reservas;
        this.publicador = publicador;
        this.relogio = relogio;
        this.validade = validade;
    }

    // Consumidor idempotente de EventoPublicado, cria a Disponibilidade de cada categoria uma única vez
    @Transactional
    public void registrarDisponibilidades(EventoPublicado evento) {
        for (EventoPublicado.CategoriaPublicada c : evento.categorias()) {
            if (!disponibilidades.existe(c.categoriaId())) {
                disponibilidades.salvar(Disponibilidade.criar(c.categoriaId(), evento.eventoId(), c.nome(), Dinheiro.deCentavos(c.precoEmCentavos()), c.capacidade(), Periodo.de(c.inicioDasVendas(), c.fimDasVendas())));
            }
        }
    }

    @Transactional
    public Reserva reservar(ReservarIngressos cmd) {
        Disponibilidade disponibilidade = disponibilidades.buscarParaAtualizacao(cmd.categoriaId()).orElseThrow(() -> new NaoEncontradoException("disponibilidade.nao-encontrada", "Categoria não encontrada ou evento não publicado: " + cmd.categoriaId()));
        Instant agora = relogio.instant();
        Reserva reserva = Reserva.criar(cmd.usuarioId(), disponibilidade, cmd.quantidade(), agora, validade);
        disponibilidades.salvar(disponibilidade);
        reservas.salvar(reserva);
        publicador.publicar(new ReservaCriada(Ids.novo(), agora, reserva.id(), reserva.usuarioId(), reserva.eventoId(), reserva.valorTotal().centavos(), reserva.expiraEm()));
        return reserva;
    }

    // Expira uma reserva vencida. Cada chamada é uma transação própria. 
    @Transactional
    public boolean expirar(UUID reservaId) {
        Reserva reserva = reservas.buscarParaAtualizacao(reservaId).orElse(null);
        if (reserva == null) {
            return false;
        }
        Disponibilidade disponibilidade = disponibilidades.buscarParaAtualizacao(reserva.categoriaId()).orElseThrow(() -> new IllegalStateException("Reserva sem disponibilidade: " + reserva.categoriaId()
            ));
        Instant agora = relogio.instant();
        if (!reserva.expirar(agora, disponibilidade)) {
            return false;
        }        
        disponibilidades.salvar(disponibilidade);
        reservas.salvar(reserva);
        publicador.publicar(new ReservaExpirada(Ids.novo(), agora, reserva.id()));
        return true;
    }

    @Transactional
    public Reserva confirmar(UUID reservaId) {
        Reserva reserva = reservas.buscarParaAtualizacao(reservaId).orElseThrow(() -> new NaoEncontradoException("reserva.nao-encontrada", "Reserva não encontrada: " + reservaId));
        boolean jaConfirmada = reserva.status() == StatusReserva.CONFIRMADA;
        Instant agora = relogio.instant();
        reserva.confirmar(agora);
        if (jaConfirmada) {
            return reserva;
        }
        reservas.salvar(reserva);
        publicador.publicar(new ReservaConfirmada(Ids.novo(), agora, reserva.id(), reserva.usuarioId(), reserva.eventoId(), reserva.categoriaId(), reserva.quantidade()));
        return reserva;
    }

    @Transactional(readOnly = true)
    public List<UUID> reservasVencidas() {
        return reservas.idsVencidas(relogio.instant(), LOTE_DE_EXPIRACAO);
    }

    @Transactional(readOnly = true)
    public Reserva buscar(UUID id) {
        return reservas.buscar(id).orElseThrow(() -> new NaoEncontradoException("reserva.nao-encontrada", "Reserva não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public Reserva buscarDoUsuario(UUID id, UUID usuarioId) {
        Reserva reserva = buscar(id);
        if (!reserva.usuarioId().equals(usuarioId)) {
            throw new AcessoNegadoException("reserva.de-outro-usuario", "Esta reserva pertence a outro usuário");
        }
        return reserva;
    }

    @Transactional(readOnly = true)
    public List<Disponibilidade> disponibilidadesDoEvento(UUID eventoId) {
        return disponibilidades.listarPorEvento(eventoId);
    }
}
