package br.pucrs.tcc.bilheteria.compartilhado.infra.seguranca;

// Nomes dos campos do token. Quem emite (Usuários) e quem valida (a configuração de segurança) usam os mesmos. 
public final class ClaimsDoToken {

    public static final String EMISSOR = "bilheteria";
    public static final String PAPEL = "papel";

    private ClaimsDoToken() {
    }
}