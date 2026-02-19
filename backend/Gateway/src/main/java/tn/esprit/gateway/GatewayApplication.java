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

    @Value("${gateway.service.evaluation:EVALUATION}")
    private String evaluationServiceId;

    @Value("${gateway.service.formation:FORMATION}")
    private String formationServiceId;

    @Value("${gateway.service.skill:SKILL}")
    private String skillServiceId;

    @Value("${gateway.service.project:PROJECT}")
    private String projectServiceId;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouting(RouteLocatorBuilder builder, NotFoundJsonFilter notFoundJsonFilter, WelcomeJsonFilter welcomeJsonFilter) {
        return builder.routes()
                // Examens et Certificats -> lb://EVALUATION (nom dans application.properties)
                .route("evaluation-examens", r -> r.path("/api/examens", "/api/examens/**")
                        .uri("lb://" + evaluationServiceId))
                .route("evaluation-certificats", r -> r.path("/api/certificats", "/api/certificats/**")
                        .uri("lb://" + evaluationServiceId))
                // Formations / Inscriptions -> lb://FORMATION (nom dans application.properties)
                .route("formation-api", r -> r.path("/api/formations", "/api/formations/**")
                        .uri("lb://" + formationServiceId))
                .route("formation-inscriptions", r -> r.path("/api/inscriptions", "/api/inscriptions/**")
                        .uri("lb://" + formationServiceId))
                // Skill -> lb://SKILL (/api/skills -> /skills, /api/portfolio -> /portfolio, etc.)
                .route("skill-api", r -> r.path("/api/skills", "/api/skills/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + skillServiceId))
                .route("skill-portfolio", r -> r.path("/api/portfolio", "/api/portfolio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + skillServiceId))
                .route("skill-bio", r -> r.path("/api/bio", "/api/bio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + skillServiceId))
                .route("skill-cv", r -> r.path("/api/cv", "/api/cv/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + skillServiceId))
                // Project -> lb://PROJECT (/api/projects -> /projects)
                .route("project-api", r -> r.path("/api/projects", "/api/projects/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + projectServiceId))
                // Racine : uniquement / et /api (pas /api/xxx) -> reponse JSON
                .route("gateway-welcome", r -> r.path("/", "/api")
                        .and().method(org.springframework.http.HttpMethod.GET)
                        .filters(f -> f.filter(welcomeJsonFilter))
                        .uri("http://localhost:8082"))

                // Fallback : toute autre URL -> 404 en JSON (plus de Whitelabel)
                .route("fallback-404", r -> r.order(Ordered.LOWEST_PRECEDENCE)
                        .path("/**")
                        .filters(f -> f.filter(notFoundJsonFilter))
                        .uri("http://localhost:8082"))
                .build();
    }
}

