package br.pucrs.tcc.bilheteria.pagamentos.domain;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

/*
Agregado raiz de Pagamentos. Máquina de estados:
INICIADO -> APROVADO | RECUSADO | CANCELADO ; APROVADO -> ESTORNADO.
RECUSADO, CANCELADO e ESTORNADO são estados finais.
*/
public class Pagamento {

    private final UUID id;
    private final UUID reservaId;
    private final String chaveDeIdempotencia;
    private final Dinheiro valor;
    private final Instant criadoEm;
    private StatusPagamento status;
    private String motivo;
    private String codigoNoGateway;

    private Pagamento(UUID id, UUID reservaId, String chaveDeIdempotencia, Dinheiro valor, Instant criadoEm, StatusPagamento status, String motivo, String codigoNoGateway) {
        this.id = id;
        this.reservaId = reservaId;
        this.chaveDeIdempotencia = chaveDeIdempotencia;
        this.valor = valor;
        this.criadoEm = criadoEm;
        this.status = status;
        this.motivo = motivo;
        this.codigoNoGateway = codigoNoGateway;
    }

    public static Pagamento iniciar(ReservaRef reserva, String chaveDeIdempotencia, Instant agora) {
        if (chaveDeIdempotencia == null || chaveDeIdempotencia.isBlank()) {
            throw new RegraDeNegocioException("pagamento.chave-obrigatoria", "A chave de idempotência é obrigatória");
        }
        if (!reserva.aceitaPagamento(agora)) {
            throw new ConflitoException("reserva.expirada", "O prazo para pagar esta reserva já venceu");
        }
        return new Pagamento(Ids.novo(), reserva.id(), chaveDeIdempotencia, reserva.valor(), agora, StatusPagamento.INICIADO, null, null);
    }


    public static Pagamento reconstituir(UUID id, UUID reservaId, String chaveDeIdempotencia, Dinheiro valor, Instant criadoEm, StatusPagamento status, String motivo, String codigoNoGateway) {
        return new Pagamento(id, reservaId, chaveDeIdempotencia, valor, criadoEm, status, motivo, codigoNoGateway);
    }

    public void aprovar(String codigoNoGateway) {
        exigirIniciado();
        this.status = StatusPagamento.APROVADO;
        this.codigoNoGateway = codigoNoGateway;
    }

    public void recusar(String motivo) {
        exigirIniciado();
        this.status = StatusPagamento.RECUSADO;
        this.motivo = motivo;
    }

    public boolean aoExpirarReserva() {
        if (status == StatusPagamento.INICIADO) {
            status = StatusPagamento.CANCELADO;
            return true;
        }
        if (status == StatusPagamento.APROVADO) {
            status = StatusPagamento.ESTORNADO;
            return true;
        }
        return false;
    }

    private void exigirIniciado() {
        if (status != StatusPagamento.INICIADO) {
            throw new ConflitoException("pagamento.transicao-invalida", "Operação inválida para um pagamento " + status);
        }
    }

    public UUID id() { return id; }
    public UUID reservaId() { return reservaId; }
    public String chaveDeIdempotencia() { return chaveDeIdempotencia; }
    public Dinheiro valor() { return valor; }
    public Instant criadoEm() { return criadoEm; }
    public StatusPagamento status() { return status; }
    public String motivo() { return motivo; }
    public String codigoNoGateway() { return codigoNoGateway; }
}