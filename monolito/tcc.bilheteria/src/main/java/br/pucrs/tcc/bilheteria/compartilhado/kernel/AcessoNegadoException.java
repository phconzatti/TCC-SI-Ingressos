package br.pucrs.tcc.bilheteria.compartilhado.kernel;

public class AcessoNegadoException extends ExcecaoDeDominio {

    public AcessoNegadoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}