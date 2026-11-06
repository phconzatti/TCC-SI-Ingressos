package br.pucrs.tcc.bilheteria.pagamentos.application;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.pucrs.tcc.bilheteria.compartilhado.contratos.PagamentoAprovado;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaCriada;
import br.pucrs.tcc.bilheteria.compartilhado.contratos.ReservaExpirada;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.AcessoNegadoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.PublicadorDeEventos;
import br.pucrs.tcc.bilheteria.pagamentos.domain.Pagamento;
import br.pucrs.tcc.bilheteria.pagamentos.domain.ReservaRef;

@Service
public class PagamentoService {

    private final RepositorioDeReservasRef reservas;
    private final RepositorioDePagamentos pagamentos;
    private final GatewayDePagamento gateway;
    private final PublicadorDeEventos publicador;
    private final Clock relogio;

    public PagamentoService(RepositorioDeReservasRef reservas, RepositorioDePagamentos pagamentos, GatewayDePagamento gateway, PublicadorDeEventos publicador, Clock relogio) {
        this.reservas = reservas;
        this.pagamentos = pagamentos;
        this.gateway = gateway;
        this.publicador = publicador;
        this.relogio = relogio;
    }

    @Transactional
    public void registrarReserva(ReservaCriada evento) {
        if (!reservas.existe(evento.reservaId())) {
            reservas.salvar(ReservaRef.criar(evento.reservaId(), evento.usuarioId(), evento.eventoId(), Dinheiro.deCentavos(evento.valorEmCentavos()), evento.expiraEm()));
        }
    }

        @Transactional
    public ResultadoDoPagamento pagar(PagarReserva cmd) {
        ReservaRef reserva = reservas.buscar(cmd.reservaId()).orElseThrow(() -> new ConflitoException(
                "reserva.indisponivel-para-pagamento",
                "Reserva não encontrada ou ainda não disponível para pagamento. Tente novamente."));
        if (!reserva.usuarioId().equals(cmd.usuarioId())) {
            throw new AcessoNegadoException("reserva.de-outro-usuario", "Esta reserva pertence a outro usuário");
        }

        Optional<Pagamento> existente = pagamentos.buscarPorChave(cmd.chaveDeIdempotencia());
        if (existente.isPresent()) {
            if (!existente.get().reservaId().equals(cmd.reservaId())) {
                throw new ConflitoException("idempotencia.chave-reutilizada",
                        "Esta chave de idempotência já foi usada para outra reserva");
            }
            return new ResultadoDoPagamento(existente.get(), false);
        }

        if (pagamentos.existeAtivoParaReserva(reserva.id())) {
            throw new ConflitoException("pagamento.ja-existe",
                    "Já existe um pagamento em andamento ou aprovado para esta reserva");
        }

        Pagamento pagamento = Pagamento.iniciar(reserva, cmd.chaveDeIdempotencia(), relogio.instant());
        GatewayDePagamento.ResultadoDaCobranca resultado = gateway.cobrar(pagamento.id(), pagamento.valor(), cmd.tokenDePagamento());
        if (resultado.aprovada()) {
            pagamento.aprovar(resultado.codigo());
        } else {
            pagamento.recusar(resultado.motivo());
        }
        pagamentos.salvar(pagamento);

        if (resultado.aprovada()) {
            publicador.publicar(new PagamentoAprovado(Ids.novo(), relogio.instant(), pagamento.id(), pagamento.reservaId(), pagamento.valor().centavos()));
        }
        return new ResultadoDoPagamento(pagamento, true);
    }

    @Transactional
    public void aoReservaExpirar(ReservaExpirada evento) {
        reservas.buscar(evento.reservaId()).ifPresent(r -> {
            r.marcarExpirada();
            reservas.salvar(r);
        });
        for (Pagamento p : pagamentos.listarPorReserva(evento.reservaId())) {
            if (p.aoExpirarReserva()) {
                pagamentos.salvar(p);
            }
        }
    }

    @Transactional(readOnly = true)
    public Pagamento buscar(UUID id) {
        return pagamentos.buscar(id).orElseThrow(
                () -> new NaoEncontradoException("pagamento.nao-encontrado", "Pagamento não encontrado: " + id));
    }
}