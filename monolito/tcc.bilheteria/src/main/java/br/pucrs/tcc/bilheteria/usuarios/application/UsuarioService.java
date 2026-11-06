package br.pucrs.tcc.bilheteria.usuarios.application;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.ConflitoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoAutenticadoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.NaoEncontradoException;
import br.pucrs.tcc.bilheteria.compartilhado.kernel.RegraDeNegocioException;
import br.pucrs.tcc.bilheteria.usuarios.domain.Usuario;

@Service
public class UsuarioService {

    static final int TAMANHO_MINIMO_DA_SENHA = 8;

    private final RepositorioDeUsuarios usuarios;
    private final CodificadorDeSenha codificador;
    private final EmissorDeToken emissor;
    private final Clock relogio;

    // Hash de uma senha que ninguém tem: usado para gastar o mesmo tempo quando o e-mail não existe.
    private final String hashDeComparacao;

    public UsuarioService(RepositorioDeUsuarios usuarios, CodificadorDeSenha codificador, EmissorDeToken emissor, Clock relogio) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.emissor = emissor;
        this.relogio = relogio;
        this.hashDeComparacao = codificador.codificar("senha-que-ninguem-possui");
    }

    @Transactional
    public Usuario registrar(RegistrarUsuario cmd) {
        if (cmd.senha() == null || cmd.senha().length() < TAMANHO_MINIMO_DA_SENHA) {
            throw new RegraDeNegocioException("usuario.senha-fraca", "A senha deve ter ao menos " + TAMANHO_MINIMO_DA_SENHA + " caracteres");
        }
        String email = Usuario.normalizarEmail(cmd.email());
        if (email != null && usuarios.existePorEmail(email)) {
            throw new ConflitoException("usuario.email-ja-cadastrado", "Este e-mail já está cadastrado");
        }
        Usuario usuario = Usuario.criar(cmd.nome(), cmd.email(), codificador.codificar(cmd.senha()), cmd.papel(), relogio.instant());
        usuarios.salvar(usuario);
        return usuario;
    }

    /*
    Confere as credenciais e emite o token. A mesma resposta serve para e-mail inexistente e senha errada,
    para não revelar quais e-mails estão cadastrados.
    */
    @Transactional(readOnly = true)
    public TokenDeAcesso autenticar(String email, String senha) {
        String chave = Usuario.normalizarEmail(email);
        Optional<Usuario> usuario = (chave == null || chave.isBlank()) ? Optional.empty() : usuarios.buscarPorEmail(chave);
        String hash = usuario.map(Usuario::senhaHash).orElse(hashDeComparacao);
        boolean senhaConfere = senha != null && codificador.confere(senha, hash);
        if (usuario.isEmpty() || !senhaConfere) {
            throw new NaoAutenticadoException("credenciais.invalidas", "E-mail ou senha inválidos");
        }
        return emissor.emitir(usuario.get().id(), usuario.get().papel());
    }

    @Transactional(readOnly = true)
    public Usuario buscar(UUID id) {
        return usuarios.buscar(id).orElseThrow(() -> new NaoEncontradoException("usuario.nao-encontrado", "Usuário não encontrado"));
    }

    @Transactional(readOnly = true)
    public boolean existe(String email) {
        String chave = Usuario.normalizarEmail(email);
        return chave != null && usuarios.existePorEmail(chave);
    }
}