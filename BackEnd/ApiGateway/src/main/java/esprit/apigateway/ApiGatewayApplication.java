<<<<<<< HEAD
package esprit.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    @Value("${skill.service.uri:lb://SKILL}")
    private String skillServiceUri;

    @Value("${project.service.uri:lb://PROJECT}")
    private String projectServiceUri;

    @Value("${user.service.uri:lb://USER}")
    private String userServiceUri;

    @Value("${formation.service.uri:lb://FORMATION}")
    private String formationServiceUri;

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {

        return builder.routes()

                // SKILL SERVICE
                .route("skill-service", r -> r
                        .path("/api/skills/**")
                        .filters(f -> f.stripPrefix(1)) // enlève /api
                        .uri(skillServiceUri))

                // CV SERVICE
                .route("cv-service", r -> r
                        .path("/api/cv/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(skillServiceUri))

                // PORTFOLIO SERVICE
                .route("portfolio-service", r -> r
                        .path("/api/portfolio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(skillServiceUri))

                // BIO (Professional summary)
                .route("bio-service", r -> r
                        .path("/api/bio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(skillServiceUri))

                // PROJECT SERVICE
                .route("project-service", r -> r
                        .path("/api/projects/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(projectServiceUri))

                // USER SERVICE (microservice collègue)
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri(userServiceUri))

                // FORMATION SERVICE : le microservice attend /api/formations (ne pas enlever /api)
                .route("formation-service", r -> r
                        .path("/api/formations/**")
                        .uri(formationServiceUri))

                // EXAMEN SERVICE (même microservice Formation, attend /api/examens)
                .route("examen-service", r -> r
                        .path("/api/examens/**")
                        .uri(formationServiceUri))

                // INSCRIPTION SERVICE (attend /api/inscriptions)
                .route("inscription-service", r -> r
                        .path("/api/inscriptions/**")
                        .uri(formationServiceUri))

                .build();
    }

}
=======
package esprit.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator gatewayRoutes(
            RouteLocatorBuilder builder,
            @Value("${team.ai.url:http://127.0.0.1:5000}") String teamAiUrl) {

        return builder.routes()

                // Team IA (Python) — /api/ai/** réécrit en /api/** côté service (port 5000 par défaut)
                .route("team-ai-service", r -> r
                        .path("/api/ai/**")
                        .filters(f -> f.rewritePath("^/api/ai/(?<path>.*)$", "/api/${path}"))
                        .uri(teamAiUrl))

                // SKILL SERVICE (/api/skills seul = liste ; /api/skills/** sinon)
                .route("skill-service", r -> r
                        .path("/api/skills", "/api/skills/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://SKILL"))

                // CV SERVICE
                .route("cv-service", r -> r
                        .path("/api/cv/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://SKILL"))

                // PORTFOLIO SERVICE
                .route("portfolio-service", r -> r
                        .path("/api/portfolio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://SKILL"))

                // BIO (Professional summary)
                .route("bio-service", r -> r
                        .path("/api/bio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://SKILL"))

                // PROJECT SERVICE
                .route("project-service", r -> r
                        .path("/api/projects/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://PROJECT"))

                // USER SERVICE — ne pas stripPrefix : les contrôleurs sont sous /api/users et /api/auth
                .route("user-auth-service", r -> r
                        .path("/api/auth/**")
                        .uri("lb://USER"))
                // Liste publique GET /api/users?role=… (matching) : route dédiée en priorité (order négatif).
                .route("user-service-users-exact", r -> r
                        .order(-1)
                        .path("/api/users")
                        .uri("lb://USER"))
                .route("user-service-users", r -> r
                        .path("/api/users/**")
                        .uri("lb://USER"))

                // FORMATION SERVICE
                .route("formation-service", r -> r
                        .path("/api/formations/**")
                        .uri("lb://FORMATION"))

                // EXAMEN SERVICE
                .route("examen-service", r -> r
                        .path("/api/examens/**")
                        .uri("lb://FORMATION"))

                // INSCRIPTION SERVICE
                .route("inscription-service", r -> r
                        .path("/api/inscriptions/**")
                        .uri("lb://FORMATION"))

                // CANDIDATURE
                .route("candidature-service", r -> r
                        .path("/api/candidatures", "/api/candidatures/**")
                        .uri("lb://CANDIDATURE"))

                // CONTRACT
                .route("contract-service", r -> r
                        .path("/api/contracts", "/api/contracts/**")
                        .uri("lb://CONTRACT"))

                .route("interview-service", r -> r
                        .path("/api/interviews", "/api/interviews/**")
                        .uri("lb://INTERVIEW"))

                .build();
    }

}
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
