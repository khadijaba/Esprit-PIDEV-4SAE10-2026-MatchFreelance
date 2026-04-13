package esprit.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Décode le JWT Keycloak via le JWKS (signature), sans imposer que le claim {@code iss}
 * soit identique à une URL interne Docker ({@code keycloak:8080}) alors que le navigateur
 * obtient un token avec {@code iss=http://localhost:8180/realms/...}.
 */
@Configuration
public class KeycloakJwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${keycloak.jwk-set-uri:http://localhost:8180/realms/matchfreelancer/protocol/openid-connect/certs}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
