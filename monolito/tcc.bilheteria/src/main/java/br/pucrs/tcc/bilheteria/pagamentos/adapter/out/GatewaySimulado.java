package br.pucrs.tcc.bilheteria.pagamentos.adapter.out;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Dinheiro;
import br.pucrs.tcc.bilheteria.pagamentos.application.GatewayDePagamento;

// Simula o provedor de pagamento. Token "cartao-recusado" é recusado; qualquer outro é aprovado
@Component
class GatewaySimulado implements GatewayDePagamento {

    static final String TOKEN_RECUSADO = "cartao-recusado";

    private final Duration latencia;

    GatewaySimulado(@Value("${bilheteria.pagamentos.gateway.latencia:PT0S}") Duration latencia) {
        this.latencia = latencia;
    }

    @Override
    public ResultadoDaCobranca cobrar(UUID pagamentoId, Dinheiro valor, String tokenDePagamento) {
        if (!latencia.isZero()) {
            try {
                Thread.sleep(latencia.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Cobrança interrompida", e);
            }
        }
        if (TOKEN_RECUSADO.equals(tokenDePagamento)) {
            return ResultadoDaCobranca.recusada("Cartão recusado");
        }
        return ResultadoDaCobranca.aprovada("SIM-" + pagamentoId);
    }
}