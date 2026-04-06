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

    @Value("${candidature.service.uri:lb://CANDIDATURE}")
    private String candidatureServiceUri;

    @Value("${contract.service.uri:lb://CONTRACT}")
    private String contractServiceUri;

    @Value("${interview.service.uri:lb://INTERVIEW}")
    private String interviewServiceUri;

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

                // CANDIDATURE : contrôleurs sous /api/candidatures (ne pas stripPrefix).
                // Inclure /api/candidatures seul pour POST (certaines versions ne matchent pas /** sans segment).
                .route("candidature-service", r -> r
                        .path("/api/candidatures", "/api/candidatures/**")
                        .uri(candidatureServiceUri))

                // CONTRACT : même logique pour POST /api/contracts
                .route("contract-service", r -> r
                        .path("/api/contracts", "/api/contracts/**")
                        .uri(contractServiceUri))

                .route("interview-service", r -> r
                        .path("/api/interviews", "/api/interviews/**")
                        .uri(interviewServiceUri))

                .build();
    }

}
