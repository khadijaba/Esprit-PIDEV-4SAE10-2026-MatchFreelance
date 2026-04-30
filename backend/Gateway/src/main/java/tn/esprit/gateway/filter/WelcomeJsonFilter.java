package tn.esprit.gateway;

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
            {"service":"Gateway","message":"Plateforme Freelancers — formations et évaluations.","routes":{"users":"POST /api/users/auth/register, POST /api/users/auth/login, GET /api/users/me/profile, GET /api/users","formations":"GET/POST/PUT/DELETE /api/formations et /api/formations/{id}","inscriptions":"/api/inscriptions/...","examens":"/api/examens/...","certificats":"/api/certificats/...","candidatures":"/api/candidatures/**","contracts":"/api/contracts/**","interviews":"/api/interviews/**","productivity":"/api/productivity/goals, /api/productivity/tasks, /api/productivity/todos, /api/productivity/decisions","evaluationReports":"Python evaluation-reports-py : POST /api/evaluation-reports/charts/histogram, charts/evolution, reports/pdf, exam/auto-generate — proxy Gateway vers gateway.evaluation-reports.url (defaut localhost:8090)"}}
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
