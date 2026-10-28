package br.pucrs.tcc.bilheteria.reservas.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.EventoPublicado;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaCriada;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Periodo;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.PublicadorDeEventos;
import br.pucrs.tcc.bilheteria.reservas.domain.Disponibilidade;
import br.pucrs.tcc.bilheteria.reservas.domain.Reserva;

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
        Disponibilidade disponibilidade = disponibilidades.buscarParaAtualizacao(cmd.categoriaId())
            .orElseThrow(() -> new NaoEncontradoException("disponibilidade.nao-encontrada", "Categoria não encontrada ou evento não publicado: " + cmd.categoriaId()));
        Instant agora = relogio.instant();
        Reserva reserva = Reserva.criar(cmd.usuarioId(), disponibilidade, cmd.quantidade(), agora, validade);
        disponibilidades.salvar(disponibilidade);
        reservas.salvar(reserva);
        publicador.publicar(new ReservaCriada(Ids.novo(), agora, reserva.id(), reserva.usuarioId(), reserva.eventoId(), reserva.valorTotal().centavos(), reserva.expiraEm()));
        return reserva;
    }
}
