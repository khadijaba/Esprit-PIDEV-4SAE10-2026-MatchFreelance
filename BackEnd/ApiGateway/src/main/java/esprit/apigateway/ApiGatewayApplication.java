package esprit.apigateway;

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
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {

        return builder.routes()

                // SKILL SERVICE
                .route("skill-service", r -> r
                        .path("/api/skills/**")
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

                // USER SERVICE
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.stripPrefix(1))
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
