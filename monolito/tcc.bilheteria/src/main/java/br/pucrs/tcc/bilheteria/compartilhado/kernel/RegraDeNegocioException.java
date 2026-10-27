package br.pucrs.tcc.bilheteria.compartilhado.kernel;

// Regra de negócio violada (HTTP 422)
public class RegraDeNegocioException extends ExcecaoDeDominio{
    
    public RegraDeNegocioException(String codigo, String mensagem) {
        super(codigo, mensagem);
    } 
}
