package tn.esprit.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import tn.esprit.gateway.filter.NotFoundJsonFilter;
import tn.esprit.gateway.filter.WelcomeJsonFilter;


@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    @Value("${gateway.service.user:USER}")
    private String userServiceId;

    @Value("${gateway.service.evaluation:EVALUATION}")
    private String evaluationServiceId;

    @Value("${gateway.service.formation:FORMATION}")
    private String formationServiceId;

    @Value("${gateway.service.skill:SKILL}")
    private String skillServiceId;

    @Value("${gateway.service.project:PROJECT}")
    private String projectServiceId;

    /** URL du microservice Python (rapports PDF / graphiques) — pas Eureka, load balancer direct. */
    @Value("${gateway.evaluation-reports.url:http://localhost:8090}")
    private String evaluationReportsUrl;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouting(RouteLocatorBuilder builder, NotFoundJsonFilter notFoundJsonFilter, WelcomeJsonFilter welcomeJsonFilter) {
        return builder.routes()
                // Rapports PDF & graphiques (Python FastAPI) — en premier pour éviter tout conflit de prédicat
                .route("evaluation-reports", r -> r.path("/api/evaluation-reports", "/api/evaluation-reports/**")
                        .uri(evaluationReportsUrl))
                // Utilisateurs / Auth (legacy + v2) -> lb://USER
                .route("user-users", r -> r.path("/api/users", "/api/users/**")
                        .uri("lb://" + userServiceId))
                .route("user-auth", r -> r.path("/api/auth", "/api/auth/**")
                        .uri("lb://" + userServiceId))
                .route("user-admin", r -> r.path("/api/admin", "/api/admin/**")
                        .uri("lb://" + userServiceId))
                .route("user-uploads", r -> r.path("/uploads", "/uploads/**")
                        .uri("lb://" + userServiceId))
                // Examens et Certificats -> lb://EVALUATION (nom dans application.properties)
                // Un seul motif ** évite les ambiguïtés de matching sur les sous-chemins (ex. /formation/.../auto-generate)
                .route("evaluation-examens", r -> r.path("/api/examens/**")
                        .uri("lb://" + evaluationServiceId))
                .route("evaluation-examens-root", r -> r.path("/api/examens")
                        .uri("lb://" + evaluationServiceId))
                .route("evaluation-certificats", r -> r.path("/api/certificats/**")
                        .uri("lb://" + evaluationServiceId))
                .route("evaluation-certificats-root", r -> r.path("/api/certificats")
                        .uri("lb://" + evaluationServiceId))
                // Formations / Inscriptions / Modules -> lb://FORMATION
                .route("formation-api", r -> r.path("/api/formations", "/api/formations/**")
                        .uri("lb://" + formationServiceId))
                .route("formation-modules", r -> r.path("/api/modules", "/api/modules/**")
                        .uri("lb://" + formationServiceId))
                .route("formation-inscriptions", r -> r.path("/api/inscriptions", "/api/inscriptions/**")
                        .uri("lb://" + formationServiceId))
                // Skills / Parcours Intelligent -> lb://SKILL
                .route("skill-api", r -> r.path("/api/skills", "/api/skills/**")
                        .uri("lb://" + skillServiceId))
                // Projets (microservice Project) : /api/projects -> /projects côté service
                .route("project-api", r -> r.path("/api/projects", "/api/projects/**")
                        .filters(f -> f.rewritePath("/api/projects(?<segment>/?.*)", "/projects${segment}"))
                        .uri("lb://" + projectServiceId))
                // Racine : uniquement / et /api (pas /api/xxx) -> reponse JSON
                .route("gateway-welcome", r -> r.path("/", "/api")
                        .and().method(org.springframework.http.HttpMethod.GET)
                        .filters(f -> f.filter(welcomeJsonFilter))
                        .uri("http://localhost:8050"))

                // Fallback : toute autre URL -> 404 en JSON (plus de Whitelabel)
                .route("fallback-404", r -> r.order(Ordered.LOWEST_PRECEDENCE)
                        .path("/**")
                        .filters(f -> f.filter(notFoundJsonFilter))
                        .uri("http://localhost:8050"))
                .build();
    }
}

