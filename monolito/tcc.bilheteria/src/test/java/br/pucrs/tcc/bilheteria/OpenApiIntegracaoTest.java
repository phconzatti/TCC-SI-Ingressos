package br.pucrs.tcc.bilheteria;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "bilheteria.reservas.expiracao-ativa=false")
class OpenApiIntegracaoTest {

    @Value("${local.server.port}")
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String caminho) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build(),
                BodyHandlers.ofString());
    }

    @Test
    void contratoDescreveAsRotasOEsquemaBearerENaoVazaDadosSensiveis() throws Exception {
        HttpResponse<String> resposta = get("/v3/api-docs");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body())
                .contains("/reservas", "/pagamentos", "/ingressos", "/usuarios/eu", "/auth/login")
                .contains("check-in")
                .contains("Idempotency-Key")
                .contains("bearerAuth")
                .doesNotContain("senhaHash");
    }

    @Test
    void swaggerUiEstaDisponivel() throws Exception {
        assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(200);
    }
}