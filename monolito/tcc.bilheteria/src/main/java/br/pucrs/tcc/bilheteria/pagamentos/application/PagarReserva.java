package br.pucrs.tcc.bilheteria.pagamentos.application;

import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

public record PagarReserva(UUID reservaId, String chaveDeIdempotencia, String tokenDePagamento) {

    public PagarReserva {
        if (reservaId == null) {
            throw new RegraDeNegocioException("pagamento.reserva-obrigatoria", "reservaId é obrigatório");
        }
        if (chaveDeIdempotencia == null || chaveDeIdempotencia.isBlank()) {
            throw new RegraDeNegocioException("pagamento.chave-obrigatoria", "A chave de idempotência é obrigatória");
        }
        if (tokenDePagamento == null || tokenDePagamento.isBlank()) {
            throw new RegraDeNegocioException("pagamento.token-obrigatorio", "tokenDePagamento é obrigatório");
        }
    }
}