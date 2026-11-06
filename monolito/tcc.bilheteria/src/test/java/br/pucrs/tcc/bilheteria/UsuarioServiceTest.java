package br.pucrs.tcc.bilheteria;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import br.pucrs.tcc.bilheteria.usuarios.application.CodificadorDeSenha;
import br.pucrs.tcc.bilheteria.usuarios.application.EmissorDeToken;
import br.pucrs.tcc.bilheteria.usuarios.application.RegistrarUsuario;
import br.pucrs.tcc.bilheteria.usuarios.application.RepositorioDeUsuarios;
import br.pucrs.tcc.bilheteria.usuarios.application.TokenDeAcesso;
import br.pucrs.tcc.bilheteria.usuarios.application.UsuarioService;
import br.pucrs.tcc.bilheteria.usuarios.domain.Usuario;

class UsuarioServiceTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private final Map<UUID, Usuario> banco = new HashMap<>();
    private final AtomicInteger comparacoesDeHash = new AtomicInteger();

    private final RepositorioDeUsuarios repositorio = new RepositorioDeUsuarios() {
        public void salvar(Usuario u) {
            if (existePorEmail(u.email())) {
                throw new ConflitoException("usuario.email-ja-cadastrado", "duplicado");
            }
            banco.put(u.id(), u);
        }
        public Optional<Usuario> buscar(UUID id) { return Optional.ofNullable(banco.get(id)); }
        public Optional<Usuario> buscarPorEmail(String email) {
            return banco.values().stream().filter(u -> u.email().equals(email)).findFirst();
        }
        public boolean existePorEmail(String email) {
            return banco.values().stream().anyMatch(u -> u.email().equals(email));
        }
    };

    private final CodificadorDeSenha codificador = new CodificadorDeSenha() {
        public String codificar(String senha) { return "HASH:" + new StringBuilder(senha).reverse(); }
        public boolean confere(String senha, String hash) {
            comparacoesDeHash.incrementAndGet();
            return hash.equals(codificar(senha));
        }
    };

    private final EmissorDeToken emissor = (id, papel) -> new TokenDeAcesso("tok." + id + "." + papel, 3600);

    private final UsuarioService service =
            new UsuarioService(repositorio, codificador, emissor, Clock.fixed(T0, ZoneOffset.UTC));

    private Usuario ana() {
        return service.registrar(new RegistrarUsuario("Ana", "Ana@Exemplo.com", "senha-forte-1", Papel.CLIENTE));
    }

    @Test
    void registraNormalizandoEmailESemGuardarASenhaEmClaro() {
        Usuario u = ana();

        assertThat(u.email()).isEqualTo("ana@exemplo.com");
        assertThat(u.senhaHash()).doesNotContain("senha-forte-1");
    }

    @Test
    void rejeitaEmailDuplicadoMesmoComCaixaDiferente() {
        ana();
        assertThatThrownBy(() -> service.registrar(
                new RegistrarUsuario("Outra", "ANA@exemplo.com", "outra-senha-1", Papel.CLIENTE)))
                .hasFieldOrPropertyWithValue("codigo", "usuario.email-ja-cadastrado");
    }

    @Test
    void rejeitaSenhaCurta() {
        assertThatThrownBy(() -> service.registrar(
                new RegistrarUsuario("Ana", "ana@exemplo.com", "curta", Papel.CLIENTE)))
                .hasFieldOrPropertyWithValue("codigo", "usuario.senha-fraca");
    }

    @Test
    void loginCorretoEmiteTokenComIdEPapel() {
        Usuario u = ana();

        TokenDeAcesso token = service.autenticar(" ANA@exemplo.com ", "senha-forte-1");

        assertThat(token.valor()).contains(u.id().toString()).endsWith("CLIENTE");
        assertThat(token.expiraEmSegundos()).isEqualTo(3600);
    }

    @Test
    void senhaErradaEEmailInexistenteDaoOMesmoErro() {
        ana();

        assertThatThrownBy(() -> service.autenticar("ana@exemplo.com", "errada-123"))
                .hasFieldOrPropertyWithValue("codigo", "credenciais.invalidas");
        assertThatThrownBy(() -> service.autenticar("naoexiste@exemplo.com", "qualquer-123"))
                .hasFieldOrPropertyWithValue("codigo", "credenciais.invalidas");
    }

    @Test
    void emailInexistenteTambemGastaUmaComparacaoDeHash() {
        int antes = comparacoesDeHash.get();

        assertThatThrownBy(() -> service.autenticar("naoexiste@exemplo.com", "qualquer-123"));

        assertThat(comparacoesDeHash.get()).isEqualTo(antes + 1);
    }

    @Test
    void entradasNulasOuVaziasNaoEstouram() {
        assertThatThrownBy(() -> service.autenticar(null, null))
                .hasFieldOrPropertyWithValue("codigo", "credenciais.invalidas");
        assertThatThrownBy(() -> service.autenticar("  ", "x"))
                .hasFieldOrPropertyWithValue("codigo", "credenciais.invalidas");
    }
}