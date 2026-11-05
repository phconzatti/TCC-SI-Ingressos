package br.pucrs.tcc.bilheteria.ingressos.domain;

import java.time.Instant;
import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

/*
Agregado raiz de Ingressos: um ingresso individual, identificado por um código único (o conteúdo do QR)
Uma reserva de N ingressos gera N agregados, numerados de 1 a N 
O check-in só pode acontecer uma vez
*/
public class Ingresso {

    private final UUID id;
    private final UUID reservaId;
    private final int numero;
    private final UUID eventoId;
    private final UUID categoriaId;
    private final UUID usuarioId;
    private final String codigo;
    private final Instant emitidoEm;
    private StatusIngresso status;
    private Instant utilizadoEm;

    private Ingresso(UUID id, UUID reservaId, int numero, UUID eventoId, UUID categoriaId, UUID usuarioId, String codigo, Instant emitidoEm, StatusIngresso status, Instant utilizadoEm) {
        this.id = id;
        this.reservaId = reservaId;
        this.numero = numero;
        this.eventoId = eventoId;
        this.categoriaId = categoriaId;
        this.usuarioId = usuarioId;
        this.codigo = codigo;
        this.emitidoEm = emitidoEm;
        this.status = status;
        this.utilizadoEm = utilizadoEm;
    }

    public static Ingresso emitir(UUID reservaId, int numero, UUID eventoId, UUID categoriaId, UUID usuarioId,
                                  String codigo, Instant agora) {
        if (numero < 1) {
            throw new RegraDeNegocioException("ingresso.numero-invalido", "O número do ingresso deve ser >= 1");
        }
        if (codigo == null || codigo.isBlank()) {
            throw new RegraDeNegocioException("ingresso.codigo-obrigatorio", "O código do ingresso é obrigatório");
        }
        return new Ingresso(Ids.novo(), reservaId, numero, eventoId, categoriaId, usuarioId, codigo, agora, StatusIngresso.EMITIDO, null);
    }

    public static Ingresso reconstituir(UUID id, UUID reservaId, int numero, UUID eventoId, UUID categoriaId, UUID usuarioId, String codigo, Instant emitidoEm, StatusIngresso status, Instant utilizadoEm) {
        return new Ingresso(id, reservaId, numero, eventoId, categoriaId, usuarioId, codigo, emitidoEm, status, utilizadoEm);
    }

    public void realizarCheckIn(Instant agora) {
        if (status == StatusIngresso.UTILIZADO) {
            throw new ConflitoException("ingresso.ja-utilizado", "Este ingresso já foi utilizado");
        }
        status = StatusIngresso.UTILIZADO;
        utilizadoEm = agora;
    }

    public UUID id() { return id; }
    public UUID reservaId() { return reservaId; }
    public int numero() { return numero; }
    public UUID eventoId() { return eventoId; }
    public UUID categoriaId() { return categoriaId; }
    public UUID usuarioId() { return usuarioId; }
    public String codigo() { return codigo; }
    public Instant emitidoEm() { return emitidoEm; }
    public StatusIngresso status() { return status; }
    public Instant utilizadoEm() { return utilizadoEm; }
}