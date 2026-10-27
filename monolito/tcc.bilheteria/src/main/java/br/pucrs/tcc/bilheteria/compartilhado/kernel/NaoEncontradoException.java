package br.pucrs.tcc.bilheteria.compartilhado.kernel;

// Recurso solicitado não existe (HTTP 404)
public class NaoEncontradoException extends ExcecaoDeDominio{
    
    public NaoEncontradoException(String codigo, String mensagem) {
        super (codigo, mensagem);
    }
}
