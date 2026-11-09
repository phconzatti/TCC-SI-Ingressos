package br.pucrs.tcc.bilheteria;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "bilheteria.reservas.expiracao-ativa=false",
                "management.prometheus.metrics.export.enabled=true"
        })
class ObservabilidadeIntegracaoTest {

    @Value("${local.server.port}")
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String caminho) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build(),
                BodyHandlers.ofString());
    }

    @Test
    void prometheusEPublicoETrazMetricasTecnicasEDeNegocio() throws Exception {
        get("/eventos"); // gera ao menos uma requisição medida

        HttpResponse<String> resposta = get("/actuator/prometheus");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body())
                .contains("jvm_memory_used_bytes")
                .contains("hikaricp_connections")
                .contains("http_server_requests_seconds")
                .contains("bilheteria_reservas_criadas_total")
                .contains("application=\"bilheteria-monolito\"");
    }

    @Test
    void healthEPublicoEExpoeASondaDeProntidao() throws Exception {
        assertThat(get("/actuator/health").statusCode()).isEqualTo(200);
        assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(200);
    }
}