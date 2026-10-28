package br.pucrs.tcc.bilheteria.reservas.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

/*
Agregado raiz de Reservas
Segura ingressos de uma categoria até o pagamento ou até expirar
Criar uma reserva consome a Disponibilidade, expirar devolve
A camada de aplicação garante que a Disponibilidade esteja travada (lock) durante essas operações
*/
public class Reserva {
    
    public static final int MAXIMO_POR_RESERVA = 10;

    private final UUID id;
    private final UUID usuarioId;
    private final UUID eventoId;
    private final UUID categoriaId;
    private final int quantidade;
    private final Dinheiro valorTotal;
    private final Instant criadaEm;
    private final Instant expiraEm;
    private StatusReserva status;


    private Reserva(UUID id, UUID usuarioId, UUID eventoId, UUID categoriaId, int quantidade, Dinheiro valorTotal, Instant criadaEm, Instant expiraEm, StatusReserva status) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.eventoId = eventoId;
        this.categoriaId = categoriaId;
        this.quantidade = quantidade;
        this.valorTotal = valorTotal;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
        this.status = status;
    }

    public static Reserva criar(UUID usuarioId, Disponibilidade disponibilidade, int quantidade, Instant agora, Duration validade) {
        if (quantidade < 1 || quantidade > MAXIMO_POR_RESERVA) {
            throw new RegraDeNegocioException("reserva.quantidade-invalida", "A quantidade deve estar entre 1 e " + MAXIMO_POR_RESERVA);
        }
        disponibilidade.reservar(quantidade, agora);
        return new Reserva(Ids.novo(), usuarioId, disponibilidade.eventoId(), disponibilidade.id(), quantidade, disponibilidade.preco().multiplicar(quantidade), agora, agora.plus(validade), StatusReserva.AGUARDANDO_PAGAMENTO);
    }

    public static Reserva reconstituir(UUID id, UUID usuarioId, UUID eventoId, UUID categoriaId, int quantidade, Dinheiro valorTotal, Instant criadaEm, Instant expiraEm, StatusReserva status) {
        return new Reserva(id, usuarioId, eventoId, categoriaId, quantidade, valorTotal, criadaEm, expiraEm, status);
    }

    // Confirma o pagamento. Idempotente se já confirmada e falha se expirou
    public void confirmar(Instant agora) {
        if (status == StatusReserva.CONFIRMADA) {
            return;
        }
        if (status == StatusReserva.EXPIRADA || !agora.isBefore(expiraEm)) {
            throw new ConflitoException("reserva.expirada", "O prazo de reserva já venceu");
        }
        status = StatusReserva.CONFIRMADA;
    }

    // Expira a reserva se ainda aguarda pagamento e o prazo venceu, devolvendo os ingressos
    public boolean expirar(Instant agora, Disponibilidade disponibilidade) {
        if (status != StatusReserva.AGUARDANDO_PAGAMENTO || agora.isBefore(expiraEm)) {
            return false;
        }
        disponibilidade.liberar(quantidade);
        status = StatusReserva.EXPIRADA;
        return true;
    }

    public UUID id() { return id; }
    public UUID usuarioId() { return usuarioId; }
    public UUID eventoId() { return eventoId; }
    public UUID categoriaId() { return categoriaId; }
    public int quantidade() { return quantidade; }
    public Dinheiro valorTotal() { return valorTotal; }
    public Instant criadaEm() { return criadaEm; }
    public Instant expiraEm() { return expiraEm; }
    public StatusReserva status() { return status; }
}
