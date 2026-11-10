package br.pucrs.tcc.bilheteria.compartilhado.infra.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ConfiguracaoDeOpenApi {

    static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    OpenAPI contratoDaApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bilheteria - Monólito")
                        .version("1.0")
                        .description("Sistema de reserva e venda de ingressos (estudo de caso do TCC). "
                                + "Rotas públicas: cadastro, login, vitrine de eventos e disponibilidades. "
                                + "As demais exigem o cabeçalho Authorization: Bearer <token>."))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}