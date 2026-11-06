package br.pucrs.tcc.bilheteria.usuarios.domain;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Ids;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/*
Agregado raiz de Usuários. Guarda só o hash da senha, nunca a senha.
O e-mail é a identidade de login e é sempre guardado normalizado (minúsculas, sem espaços nas pontas).
*/
public class Usuario {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UUID id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Papel papel;
    private final Instant criadoEm;

    private Usuario(UUID id, String nome, String email, String senhaHash, Papel papel, Instant criadoEm) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.criadoEm = criadoEm;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static Usuario criar(String nome, String email, String senhaHash, Papel papel, Instant agora) {
        if (nome == null || nome.isBlank() || nome.length() > 120) {
            throw new RegraDeNegocioException("usuario.nome-invalido", "O nome é obrigatório (até 120 caracteres)");
        }
        String emailNormalizado = normalizarEmail(email);
        if (emailNormalizado == null || emailNormalizado.length() > 254
                || !EMAIL.matcher(emailNormalizado).matches()) {
            throw new RegraDeNegocioException("usuario.email-invalido", "E-mail inválido");
        }
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new RegraDeNegocioException("usuario.senha-obrigatoria", "A senha é obrigatória");
        }
        if (papel == null) {
            throw new RegraDeNegocioException("usuario.papel-obrigatorio", "O papel é obrigatório");
        }
        return new Usuario(Ids.novo(), nome.trim(), emailNormalizado, senhaHash, papel, agora);
    }

    public static Usuario reconstituir(UUID id, String nome, String email, String senhaHash, Papel papel, Instant criadoEm) {
        return new Usuario(id, nome, email, senhaHash, papel, criadoEm);
    }

    public UUID id() { return id; }
    public String nome() { return nome; }
    public String email() { return email; }
    public String senhaHash() { return senhaHash; }
    public Papel papel() { return papel; }
    public Instant criadoEm() { return criadoEm; }
}