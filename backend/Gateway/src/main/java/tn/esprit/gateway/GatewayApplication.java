package tn.esprit.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;


@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    @Value("${gateway.service.user:USER}")
    private String userServiceId;

    @Value("${gateway.service.evaluation:EVALUATION}")
    private String evaluationServiceId;
    /**
     * Override optionnel pour router EVALUATION en direct quand la résolution Eureka/hostname pose problème.
     * Ex: gateway.evaluation.url=http://localhost:8083
     */
    @Value("${gateway.evaluation.url:}")
    private String evaluationDirectUrl;

    @Value("${gateway.service.formation:FORMATION}")
    private String formationServiceId;
    /**
     * Override optionnel pour router FORMATION en direct (utile si Eureka conserve une instance obsolète).
     * Ex: gateway.formation.url=http://localhost:8096
     */
    @Value("${gateway.formation.url:}")
    private String formationDirectUrl;

    @Value("${gateway.service.skill:SKILL}")
    private String skillServiceId;

    @Value("${gateway.service.project:PROJECT}")
    private String projectServiceId;

    @Value("${gateway.service.candidature:CANDIDATURE}")
    private String candidatureServiceId;

    @Value("${gateway.service.contract:CONTRACT}")
    private String contractServiceId;

    @Value("${gateway.service.interview:INTERVIEW}")
    private String interviewServiceId;

    @Value("${gateway.service.blog:BLOG}")
    private String blogServiceId;

    @Value("${gateway.service.productivity:PRODUCTIVITY}")
    private String productivityServiceId;

    /** UI blog Angular (hors Eureka), proxifiee via /blog/** */
    @Value("${gateway.blog-web.url:http://localhost:4201}")
    private String blogWebUrl;

    /** URL du microservice Python (rapports PDF / graphiques) — pas Eureka, load balancer direct. */
    @Value("${gateway.evaluation-reports.url:http://localhost:8090}")
    private String evaluationReportsUrl;

    /** Team AI (FastAPI), hors Eureka. */
    @Value("${gateway.team-ai.url:http://127.0.0.1:5000}")
    private String teamAiUrl;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouting(RouteLocatorBuilder builder, tn.esprit.gateway.NotFoundJsonFilter notFoundJsonFilter, tn.esprit.gateway.WelcomeJsonFilter welcomeJsonFilter) {
        String evaluationUri = resolveEvaluationUri();
        String formationUri = resolveFormationUri();
        return builder.routes()
                // Rapports PDF & graphiques (Python FastAPI) — en premier pour éviter tout conflit de prédicat
                .route("evaluation-reports", r -> r.path("/api/evaluation-reports", "/api/evaluation-reports/**")
                        .uri(evaluationReportsUrl))
                // Ordre explicite : même prédicat que le fallback /** — priorité plus haute (défaut 0 < LOWEST_PRECEDENCE)
                .route("team-ai", r -> r.order(-10)
                        .path("/api/team-ai", "/api/team-ai/**")
                        .filters(f -> f.rewritePath("/api/team-ai/(?<segment>.*)", "/api/${segment}"))
                        .uri(teamAiUrl))
                // Blog web app (frontend Angular) exposee via Gateway
                .route("blog-web-ui", r -> r.path("/blog", "/blog/**")
                        .filters(f -> f.rewritePath("/blog/?(?<segment>.*)", "/${segment}"))
                        .uri(blogWebUrl))
                // Blog/forum endpoints
                .route("blog-forums", r -> r.path("/api/forums", "/api/forums/**")
                        .uri("lb://" + blogServiceId))
                .route("blog-groups", r -> r.path("/api/groups", "/api/groups/**")
                        .uri("lb://" + blogServiceId))
                .route("blog-private-messages", r -> r.path("/api/messages/private", "/api/messages/private/**")
                        .uri("lb://" + blogServiceId))
                .route("blog-threads", r -> r.path("/api/threads", "/api/threads/**")
                        .uri("lb://" + blogServiceId))
                .route("blog-toxicity", r -> r.path("/api/toxicity", "/api/toxicity/**")
                        .uri("lb://" + blogServiceId))
                .route("blog-ai", r -> r.path("/api/ai", "/api/ai/**")
                        .uri("lb://" + blogServiceId))
                .route("blog-friends", r -> r.path("/api/friends", "/api/friends/**")
                        .uri("lb://" + blogServiceId))
                .route("skill-skills", r -> r.path("/api/skills", "/api/skills/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + skillServiceId))
                .route("skill-cv", r -> r.path("/api/cv", "/api/cv/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + skillServiceId))
                .route("skill-portfolio", r -> r.path("/api/portfolio", "/api/portfolio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + skillServiceId))
                .route("skill-bio", r -> r.path("/api/bio", "/api/bio/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + skillServiceId))
                .route("project-api", r -> r.path("/api/projects", "/api/projects/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://" + projectServiceId))
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
                        .uri(evaluationUri))
                .route("evaluation-examens-root", r -> r.path("/api/examens")
                        .uri(evaluationUri))
                .route("evaluation-certificats", r -> r.path("/api/certificats/**")
                        .uri(evaluationUri))
                .route("evaluation-certificats-root", r -> r.path("/api/certificats")
                        .uri(evaluationUri))
                // Formations / Inscriptions / Modules -> lb://FORMATION
                .route("formation-api", r -> r.path("/api/formations", "/api/formations/**")
                        .uri(formationUri))
                .route("formation-modules", r -> r.path("/api/modules", "/api/modules/**")
                        .uri(formationUri))
                .route("formation-inscriptions", r -> r.path("/api/inscriptions", "/api/inscriptions/**")
                        .uri(formationUri))
                // Match Freelance (PIDEV) : candidatures, contrats, entretiens — chemins /api/... inchangés côté MS
                .route("candidature-api", r -> r.path("/api/candidatures", "/api/candidatures/**")
                        .uri("lb://" + candidatureServiceId))
                .route("contract-api", r -> r.path("/api/contracts", "/api/contracts/**")
                        .uri("lb://" + contractServiceId))
                .route("interview-api", r -> r.path("/api/interviews", "/api/interviews/**")
                        .uri("lb://" + interviewServiceId))
                // Productivity : goals, tasks, todos, decisions
                .route("productivity-goals", r -> r.path("/api/productivity/goals", "/api/productivity/goals/**")
                        .uri("lb://" + productivityServiceId))
                .route("productivity-tasks", r -> r.path("/api/productivity/tasks", "/api/productivity/tasks/**")
                        .uri("lb://" + productivityServiceId))
                .route("productivity-todos", r -> r.path("/api/productivity/todos", "/api/productivity/todos/**")
                        .uri("lb://" + productivityServiceId))
                .route("productivity-decisions", r -> r.path("/api/productivity/decisions", "/api/productivity/decisions/**")
                        .uri("lb://" + productivityServiceId))
                .route("productivity-api", r -> r.path("/api/productivity", "/api/productivity/**")
                        .uri("lb://" + productivityServiceId))
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

    private String resolveFormationUri() {
        if (formationDirectUrl != null && !formationDirectUrl.isBlank()) {
            return formationDirectUrl.trim();
        }
        return "lb://" + formationServiceId;
    }

    private String resolveEvaluationUri() {
        if (evaluationDirectUrl != null && !evaluationDirectUrl.isBlank()) {
            return evaluationDirectUrl.trim();
        }
        return "lb://" + evaluationServiceId;
    }
}

