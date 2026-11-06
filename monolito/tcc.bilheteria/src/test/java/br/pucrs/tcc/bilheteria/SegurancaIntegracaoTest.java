package br.pucrs.tcc.bilheteria;

import br.pucrs.tcc.bilheteria.compartilhado.kernel.Papel;
import br.pucrs.tcc.bilheteria.usuarios.application.RegistrarUsuario;
import br.pucrs.tcc.bilheteria.usuarios.application.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "bilheteria.reservas.expiracao-ativa=false")
class SegurancaIntegracaoTest {

    @Autowired UsuarioService usuarios;

    @Value("${local.server.port}")
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> chamar(String metodo, String caminho, String token, String json,
                                        String chaveDeIdempotencia) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho));
        if (json != null) {
            req.header("Content-Type", "application/json");
        }
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (chaveDeIdempotencia != null) {
            req.header("Idempotency-Key", chaveDeIdempotencia);
        }
        BodyPublisher corpo = json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json);
        return http.send(req.method(metodo, corpo).build(), BodyHandlers.ofString());
    }

    private HttpResponse<String> chamar(String metodo, String caminho, String token, String json) throws Exception {
        return chamar(metodo, caminho, token, json, null);
    }

    private static String extrair(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!m.find()) {
            throw new AssertionError("Campo '" + campo + "' ausente em: " + json);
        }
        return m.group(1);
    }

    /** Cria um usuário com o papel dado (direto pelo serviço, já que o cadastro público só cria CLIENTE) e faz login. */
    private String tokenDeNovoUsuario(Papel papel) throws Exception {
        String email = UUID.randomUUID() + "@teste.com";
        usuarios.registrar(new RegistrarUsuario("Pessoa de Teste", email, "senha-forte-1", papel));
        HttpResponse<String> login = chamar("POST", "/auth/login", null,
                """
                {"email":"%s","senha":"senha-forte-1"}""".formatted(email));
        assertThat(login.statusCode()).isEqualTo(200);
        return extrair(login.body(), "accessToken");
    }

    @Test
    void cadastroLoginEPerfil() throws Exception {
        String email = "Ana-" + UUID.randomUUID() + "@Exemplo.com";

        HttpResponse<String> cadastro = chamar("POST", "/usuarios", null,
                """
                {"nome":"Ana","email":"%s","senha":"senha-forte-1"}""".formatted(email));
        assertThat(cadastro.statusCode()).isEqualTo(201);
        assertThat(cadastro.body()).contains(email.toLowerCase()).contains("CLIENTE").doesNotContain("senha");

        assertThat(chamar("POST", "/usuarios", null,
                """
                {"nome":"Ana","email":"%s","senha":"senha-forte-1"}""".formatted(email)).statusCode())
                .as("e-mail repetido").isEqualTo(409);
        assertThat(chamar("POST", "/usuarios", null,
                """
                {"nome":"Ana","email":"outra-%s","senha":"curta"}""".formatted(email)).statusCode())
                .as("senha curta").isEqualTo(422);

        HttpResponse<String> login = chamar("POST", "/auth/login", null,
                """
                {"email":"%s","senha":"senha-forte-1"}""".formatted(email));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("Bearer");

        HttpResponse<String> eu = chamar("GET", "/usuarios/eu", extrair(login.body(), "accessToken"), null);
        assertThat(eu.statusCode()).isEqualTo(200);
        assertThat(eu.body()).contains(email.toLowerCase());
    }

    @Test
    void loginInvalidoDa401SemRevelarSeOEmailExiste() throws Exception {
        String email = UUID.randomUUID() + "@teste.com";
        usuarios.registrar(new RegistrarUsuario("Pessoa", email, "senha-forte-1", Papel.CLIENTE));

        HttpResponse<String> senhaErrada = chamar("POST", "/auth/login", null,
                """
                {"email":"%s","senha":"errada-123"}""".formatted(email));
        HttpResponse<String> emailInexistente = chamar("POST", "/auth/login", null,
                """
                {"email":"%s","senha":"errada-123"}""".formatted("nao-existe-" + email));

        assertThat(senhaErrada.statusCode()).isEqualTo(401);
        assertThat(emailInexistente.statusCode()).isEqualTo(401);
        assertThat(senhaErrada.body()).contains("credenciais.invalidas");
        assertThat(emailInexistente.body()).isEqualTo(senhaErrada.body());
    }

    @Test
    void rotasProtegidasExigemTokenValido() throws Exception {
        String corpoReserva = """
                {"categoriaId":"%s","quantidade":1}""".formatted(UUID.randomUUID());

        assertThat(chamar("POST", "/reservas", null, corpoReserva).statusCode()).isEqualTo(401);
        assertThat(chamar("POST", "/reservas", "token-que-nao-e-jwt", corpoReserva).statusCode()).isEqualTo(401);
        assertThat(chamar("GET", "/eventos", null, null).statusCode()).as("vitrine é pública").isEqualTo(200);
        assertThat(chamar("GET", "/actuator/health", null, null).statusCode()).isEqualTo(200);
        assertThat(chamar("GET", "/rota-que-nao-existe", null, null).statusCode()).isIn(401, 403);
    }

    @Test
    void papelErradoDa403() throws Exception {
        String cliente = tokenDeNovoUsuario(Papel.CLIENTE);
        String portaria = tokenDeNovoUsuario(Papel.PORTARIA);
        String organizador = tokenDeNovoUsuario(Papel.ORGANIZADOR);
        String evento = """
                {"nome":"Show","local":"Arena","dataDoEvento":"%s","capacidade":100}"""
                .formatted(Instant.now().plus(30, ChronoUnit.DAYS));

        assertThat(chamar("POST", "/eventos", cliente, evento).statusCode()).isEqualTo(403);
        assertThat(chamar("POST", "/eventos", portaria, evento).statusCode()).isEqualTo(403);
        assertThat(chamar("POST", "/eventos", organizador, evento).statusCode()).isEqualTo(201);
        assertThat(chamar("POST", "/ingressos/qualquer/check-in", cliente, null).statusCode()).isEqualTo(403);
    }

    @Test
    void fluxoCompletoPorHttpRespeitandoODonoDeCadaRecurso() throws Exception {
        String organizador = tokenDeNovoUsuario(Papel.ORGANIZADOR);
        String clienteA = tokenDeNovoUsuario(Papel.CLIENTE);
        String clienteB = tokenDeNovoUsuario(Papel.CLIENTE);
        String portaria = tokenDeNovoUsuario(Papel.PORTARIA);
        Instant agora = Instant.now();

        // Organizador monta e publica o evento
        String eventoId = extrair(chamar("POST", "/eventos", organizador,
                """
                {"nome":"Show","local":"Arena","dataDoEvento":"%s","capacidade":100}"""
                        .formatted(agora.plus(30, ChronoUnit.DAYS))).body(), "id");
        assertThat(chamar("POST", "/eventos/" + eventoId + "/categorias", organizador,
                """
                {"nome":"Pista","precoEmCentavos":5000,"capacidade":50,"inicioDasVendas":"%s","fimDasVendas":"%s"}"""
                        .formatted(agora.minusSeconds(60), agora.plus(29, ChronoUnit.DAYS))).statusCode())
                .isEqualTo(201);
        assertThat(chamar("POST", "/eventos/" + eventoId + "/publicacao", organizador, null).statusCode())
                .isEqualTo(200);

        // A vitrine é pública
        HttpResponse<String> vitrine = chamar("GET", "/disponibilidades?eventoId=" + eventoId, null, null);
        assertThat(vitrine.statusCode()).isEqualTo(200);
        String categoriaId = extrair(vitrine.body(), "categoriaId");

        // Cliente A reserva: o dono da reserva é quem está no token
        String idDoA = extrair(chamar("GET", "/usuarios/eu", clienteA, null).body(), "id");
        HttpResponse<String> reservaA = chamar("POST", "/reservas", clienteA,
                """
                {"categoriaId":"%s","quantidade":2}""".formatted(categoriaId));
        assertThat(reservaA.statusCode()).isEqualTo(201);
        assertThat(extrair(reservaA.body(), "usuarioId")).isEqualTo(idDoA);
        String reservaId = extrair(reservaA.body(), "id");

        // Cliente B não consegue reservar "em nome de A": o usuarioId do corpo é ignorado
        String idDoB = extrair(chamar("GET", "/usuarios/eu", clienteB, null).body(), "id");
        HttpResponse<String> reservaDoB = chamar("POST", "/reservas", clienteB,
                """
                {"usuarioId":"%s","categoriaId":"%s","quantidade":1}""".formatted(idDoA, categoriaId));
        assertThat(reservaDoB.statusCode()).isEqualTo(201);
        assertThat(extrair(reservaDoB.body(), "usuarioId")).isEqualTo(idDoB);

        // B não vê nem paga a reserva de A
        assertThat(chamar("GET", "/reservas/" + reservaId, clienteB, null).statusCode()).isEqualTo(403);
        HttpResponse<String> pagamentoDoB = chamar("POST", "/pagamentos", clienteB,
                """
                {"reservaId":"%s","tokenDePagamento":"tok-ok"}""".formatted(reservaId),
                UUID.randomUUID().toString());
        assertThat(pagamentoDoB.statusCode()).isEqualTo(403);
        assertThat(pagamentoDoB.body()).contains("reserva.de-outro-usuario");

        // A vê e paga a própria reserva
        assertThat(chamar("GET", "/reservas/" + reservaId, clienteA, null).statusCode()).isEqualTo(200);
        HttpResponse<String> pagamento = chamar("POST", "/pagamentos", clienteA,
                """
                {"reservaId":"%s","tokenDePagamento":"tok-ok"}""".formatted(reservaId),
                UUID.randomUUID().toString());
        assertThat(pagamento.statusCode()).isEqualTo(201);
        assertThat(pagamento.body()).contains("APROVADO");
        assertThat(chamar("GET", "/reservas/" + reservaId, clienteA, null).body()).contains("CONFIRMADA");

        // Consulta administrativa do pagamento: só o organizador
        String pagamentoId = extrair(pagamento.body(), "id");
        assertThat(chamar("GET", "/pagamentos/" + pagamentoId, clienteA, null).statusCode()).isEqualTo(403);
        assertThat(chamar("GET", "/pagamentos/" + pagamentoId, organizador, null).statusCode()).isEqualTo(200);

        // Ingressos: o dono e a equipe veem; outro cliente não
        HttpResponse<String> ingressosDoA = chamar("GET", "/ingressos?reservaId=" + reservaId, clienteA, null);
        assertThat(ingressosDoA.statusCode()).isEqualTo(200);
        String codigo = extrair(ingressosDoA.body(), "codigo");
        assertThat(chamar("GET", "/ingressos?reservaId=" + reservaId, clienteB, null).statusCode()).isEqualTo(403);
        assertThat(chamar("GET", "/ingressos?reservaId=" + reservaId, portaria, null).statusCode()).isEqualTo(200);
        assertThat(chamar("GET", "/ingressos/" + codigo, clienteB, null).statusCode()).isEqualTo(403);

        // Check-in: o cliente não faz; a portaria faz uma única vez
        assertThat(chamar("POST", "/ingressos/" + codigo + "/check-in", clienteA, null).statusCode())
                .isEqualTo(403);
        HttpResponse<String> checkIn = chamar("POST", "/ingressos/" + codigo + "/check-in", portaria, null);
        assertThat(checkIn.statusCode()).isEqualTo(200);
        assertThat(checkIn.body()).contains("UTILIZADO");
        assertThat(chamar("POST", "/ingressos/" + codigo + "/check-in", portaria, null).statusCode())
                .isEqualTo(409);
    }
}