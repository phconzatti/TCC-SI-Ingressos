package br.pucrs.tcc.bilheteria;

import br.pucrs.tcc.bilheteria.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventoApiTest {

    @Value("${local.server.port}")
    int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String caminho, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(json))
                .build();
        return http.send(req, BodyHandlers.ofString());
    }

    @Test
    void fluxoCriarAdicionarCategoriaEPublicar() throws Exception {
        Instant data = Instant.now().plus(30, ChronoUnit.DAYS);
        Instant fim = data.minus(1, ChronoUnit.HOURS);

        HttpResponse<String> criado = post("/eventos", """
                {"nome":"Show","local":"Arena","dataDoEvento":"%s","capacidade":100}""".formatted(data));
        assertThat(criado.statusCode()).isEqualTo(201);
        String id = criado.headers().firstValue("Location").orElseThrow().substring("/eventos/".length());

        HttpResponse<String> categoria = post("/eventos/" + id + "/categorias", """
                {"nome":"Pista","precoEmCentavos":5000,"capacidade":80,
                 "inicioDasVendas":"%s","fimDasVendas":"%s"}""".formatted(Instant.now(), fim));
        assertThat(categoria.statusCode()).isEqualTo(201);

        HttpResponse<String> excedida = post("/eventos/" + id + "/categorias", """
                {"nome":"VIP","precoEmCentavos":9000,"capacidade":30,
                 "inicioDasVendas":"%s","fimDasVendas":"%s"}""".formatted(Instant.now(), fim));
        assertThat(excedida.statusCode()).isEqualTo(422);
        assertThat(excedida.body()).contains("evento.capacidade-excedida");

        HttpResponse<String> publicado = post("/eventos/" + id + "/publicacao", "");
        assertThat(publicado.statusCode()).isEqualTo(200);
        assertThat(publicado.body()).contains("PUBLICADO");

        assertThat(post("/eventos/" + id + "/publicacao", "").statusCode()).isEqualTo(409);
    }
}