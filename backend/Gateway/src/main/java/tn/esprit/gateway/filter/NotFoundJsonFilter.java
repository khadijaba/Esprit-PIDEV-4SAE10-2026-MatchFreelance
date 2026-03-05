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
 * Filtre qui repond 404 JSON sans appeler un service (pour la route fallback).
 */
@Component
public class NotFoundJsonFilter implements org.springframework.cloud.gateway.filter.GatewayFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":404,\"error\":\"Not Found\",\"path\":\"%s\",\"message\":\"Utilisez /api/users (auth), /api/projects, /api/formations, /api/inscriptions, /api/examens, /api/certificats ou / (page d'accueil)\"}",
                java.time.Instant.now(),
                path.replace("\"", "\\\"")
        );

        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
