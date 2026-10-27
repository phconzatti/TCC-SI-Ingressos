package br.pucrs.tcc.bilheteria.compartilhado.kernel;

// Base da exceções. Identifica a regra violada e a API expõe
public abstract class ExcecaoDeDominio extends RuntimeException {

    private final String codigo;

    protected ExcecaoDeDominio(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}