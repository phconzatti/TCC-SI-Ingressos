package br.pucrs.tcc.bilheteria.compartilhado.kernel;

public class NaoAutenticadoException extends ExcecaoDeDominio {

    public NaoAutenticadoException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}