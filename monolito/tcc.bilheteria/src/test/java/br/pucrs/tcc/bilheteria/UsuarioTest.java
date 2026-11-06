package br.pucrs.tcc.bilheteria;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import br.pucrs.tcc.bilheteria.usuarios.domain.Usuario;

class UsuarioTest {

    private static final Instant AGORA = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void normalizaEmailENomeEGuardaSoOHash() {
        Usuario u = Usuario.criar("  Ana Souza ", "  Ana@Exemplo.COM ", "$2a$hash", Papel.CLIENTE, AGORA);

        assertThat(u.email()).isEqualTo("ana@exemplo.com");
        assertThat(u.nome()).isEqualTo("Ana Souza");
        assertThat(u.senhaHash()).isEqualTo("$2a$hash");
        assertThat(u.papel()).isEqualTo(Papel.CLIENTE);
    }

    @Test
    void rejeitaDadosInvalidos() {
        assertThatThrownBy(() -> Usuario.criar(" ", "a@b.com", "h", Papel.CLIENTE, AGORA))
                .hasFieldOrPropertyWithValue("codigo", "usuario.nome-invalido");
        assertThatThrownBy(() -> Usuario.criar("Ana", "sem-arroba", "h", Papel.CLIENTE, AGORA))
                .hasFieldOrPropertyWithValue("codigo", "usuario.email-invalido");
        assertThatThrownBy(() -> Usuario.criar("Ana", "a@b.com", " ", Papel.CLIENTE, AGORA))
                .hasFieldOrPropertyWithValue("codigo", "usuario.senha-obrigatoria");
        assertThatThrownBy(() -> Usuario.criar("Ana", "a@b.com", "h", null, AGORA))
                .hasFieldOrPropertyWithValue("codigo", "usuario.papel-obrigatorio");
    }

    @Test
    void somentePortariaEOrganizadorSaoEquipe() {
        assertThat(Papel.CLIENTE.ehEquipe()).isFalse();
        assertThat(Papel.PORTARIA.ehEquipe()).isTrue();
        assertThat(Papel.ORGANIZADOR.ehEquipe()).isTrue();
    }
}