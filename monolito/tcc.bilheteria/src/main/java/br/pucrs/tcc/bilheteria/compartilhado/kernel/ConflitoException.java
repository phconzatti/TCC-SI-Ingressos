package br.pucrs.tcc.bilheteria.compartilhado.kernel;

// Operação conflita com estado atual (ex: reserva expirada, ingressos esgotados) (HTTP 409)
public class ConflitoException extends ExcecaoDeDominio{
    
    public ConflitoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
