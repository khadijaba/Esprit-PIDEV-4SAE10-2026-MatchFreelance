package tn.esprit.formation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client pour appeler le microservice Skill (compétences) via Eureka (lb://SKILL).
 * Utilisé pour la recommandation de formations (métier avancé).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SkillClient {

    private static final String SKILL_SERVICE = "http://SKILL";
    private final RestTemplate restTemplate;

    /**
     * Récupère les compétences du freelancer (chaque élément contient au moins "category").
     * Retourne la liste des noms de catégories (ex: WEB_DEVELOPMENT, DATA_SCIENCE).
     */
    public List<String> getCategoriesByFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> skills = restTemplate.exchange(
                    SKILL_SERVICE + "/api/skills/freelancer/" + freelancerId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            if (skills == null) return Collections.emptyList();
            return skills.stream()
                    .map(s -> s.get("category"))
                    .filter(c -> c != null)
                    .map(Object::toString)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Impossible de récupérer les compétences du freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
