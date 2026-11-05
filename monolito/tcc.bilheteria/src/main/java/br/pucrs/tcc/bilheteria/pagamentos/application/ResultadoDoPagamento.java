package br.pucrs.tcc.bilheteria.pagamentos.application;

import br.pucrs.tcc.bilheteria.pagamentos.domain.Pagamento;

public record ResultadoDoPagamento(Pagamento pagamento, boolean novo) {
}