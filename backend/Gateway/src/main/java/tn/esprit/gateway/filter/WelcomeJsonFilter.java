package tn.esprit.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Filtre qui repond en JSON pour / et /api (page d'accueil Gateway).
 */
@Component
public class WelcomeJsonFilter implements org.springframework.cloud.gateway.filter.GatewayFilter, Ordered {

    private static final String BODY = """
            {"service":"Gateway","message":"Utilisez les URLs ci-dessous.","routes":{"formations":"GET/POST/PUT/DELETE /api/formations et /api/formations/{id}","inscriptions":"/api/inscriptions/...","certificat":"/certificat/..."}}
            """;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(BODY.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
