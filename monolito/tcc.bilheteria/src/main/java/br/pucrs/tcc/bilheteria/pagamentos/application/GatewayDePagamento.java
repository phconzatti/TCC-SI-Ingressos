package br.pucrs.tcc.bilheteria.pagamentos.application;

import java.util.UUID;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;

// Porta de saída para o provedor de pagamento externo
public interface GatewayDePagamento {

    ResultadoDaCobranca cobrar(UUID pagamentoId, Dinheiro valor, String tokenDePagamento);

    record ResultadoDaCobranca(boolean aprovada, String codigo, String motivo) {
        public static ResultadoDaCobranca aprovada(String codigo) {
            return new ResultadoDaCobranca(true, codigo, null);
        }

        public static ResultadoDaCobranca recusada(String motivo) {
            return new ResultadoDaCobranca(false, null, motivo);
        }
    }
}