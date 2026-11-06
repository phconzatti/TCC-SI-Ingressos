package br.pucrs.tcc.bilheteria.compartilhado.infra.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/*
API stateless: nenhuma sessão, nenhum CSRF (não há cookie de sessão a proteger), identidade só pelo token.
Regra grossa por URL e papel aqui; a regra fina de posse fica nos serviços.
*/
@Configuration
@EnableWebSecurity
public class ConfiguracaoDeSeguranca {

    @Bean
    SecretKey chaveDoJwt(@Value("${bilheteria.seguranca.jwt.segredo}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("bilheteria.seguranca.jwt.segredo deve ter ao menos 32 bytes (HS256)");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveDoJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveDoJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chaveDoJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveDoJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acesso -> acesso
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/eventos", "/eventos/*", "/disponibilidades").permitAll()
                        .requestMatchers(HttpMethod.POST, "/eventos", "/eventos/*/categorias",
                                "/eventos/*/publicacao").hasRole("ORGANIZADOR")
                        .requestMatchers(HttpMethod.POST, "/reservas", "/pagamentos").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/reservas/*").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/pagamentos/*").hasRole("ORGANIZADOR")
                        .requestMatchers(HttpMethod.POST, "/ingressos/*/check-in")
                                .hasAnyRole("PORTARIA", "ORGANIZADOR")
                        .requestMatchers(HttpMethod.GET, "/ingressos", "/ingressos/*", "/usuarios/eu")
                                .authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeToken())));
        return http.build();
    }

    // Transforma o claim "papel" na autoridade ROLE_<papel>, que é o que hasRole(...) confere. 
    private JwtAuthenticationConverter conversorDeToken() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(ClaimsDoToken.PAPEL);
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }
}