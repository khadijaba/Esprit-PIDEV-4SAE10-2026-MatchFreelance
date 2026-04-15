package tn.esprit.evaluation.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client vers le microservice Skill (attribution de compétences freelancer).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SkillClient {

    private static final String SKILL_SERVICE = "http://SKILL";
    private final RestTemplate restTemplate;

    /**
     * Sous-ensemble de la réponse Skill {@code /skills/parcours/intelligent} (champs ignorés inconnus).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParcoursIntelligentBrief {
        private List<String> gapsDetectes;
    }

    @Getter
    public static class AttributionOutcome {
        public enum Kind { CREATED, ALREADY_EXISTS, FAILED }
        private final Kind kind;
        private final Long skillId;

        public AttributionOutcome(Kind kind, Long skillId) {
            this.kind = kind;
            this.skillId = skillId;
        }
    }

    public AttributionOutcome creerCompetenceFreelancer(
            Long freelancerId,
            String nom,
            String categorieSkill,
            String niveau,
            int anneesExperience) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new HashMap<>();
            body.put("name", nom);
            body.put("category", categorieSkill);
            body.put("freelancerId", freelancerId);
            body.put("level", niveau);
            body.put("yearsOfExperience", anneesExperience);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    SKILL_SERVICE + "/api/skills",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Map<?, ?> map = response.getBody();
            Long id = null;
            if (map != null && map.get("id") != null) {
                id = ((Number) map.get("id")).longValue();
            }
            return new AttributionOutcome(AttributionOutcome.Kind.CREATED, id);
        } catch (HttpStatusCodeException e) {
            String b = e.getResponseBodyAsString();
            if (b != null && b.toLowerCase().contains("already exists")) {
                return new AttributionOutcome(AttributionOutcome.Kind.ALREADY_EXISTS, null);
            }
            log.warn("Skill MS refus pour freelancer {}: {}", freelancerId, e.getMessage());
            return new AttributionOutcome(AttributionOutcome.Kind.FAILED, null);
        } catch (Exception e) {
            log.warn("Impossible d'appeler Skill pour freelancer {}: {}", freelancerId, e.getMessage());
            return new AttributionOutcome(AttributionOutcome.Kind.FAILED, null);
        }
    }

    /**
     * Appelle GET {@code /api/skills/parcours/intelligent?freelancerId=...} (microservice SKILL).
     * En cas d’échec (réseau, service arrêté), retourne un objet avec liste de gaps vide.
     */
    /**
     * Compétences enregistrées pour le freelancer (GET {@code /api/skills/freelancer/{id}}).
     */
    public List<Map<String, Object>> getSkillsByFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> data = restTemplate.exchange(
                    SKILL_SERVICE + "/api/skills/freelancer/" + freelancerId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return data != null ? data : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Impossible de charger les skills du freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public ParcoursIntelligentBrief getParcoursIntelligent(Long freelancerId) {
        try {
            ParcoursIntelligentBrief body = restTemplate.getForObject(
                    SKILL_SERVICE + "/api/skills/parcours/intelligent?freelancerId=" + freelancerId,
                    ParcoursIntelligentBrief.class
            );
            if (body == null || body.getGapsDetectes() == null) {
                return new ParcoursIntelligentBrief(Collections.emptyList());
            }
            return body;
        } catch (Exception e) {
            log.warn("Skill parcours intelligent indisponible pour freelancer {}: {}", freelancerId, e.getMessage());
            return new ParcoursIntelligentBrief(Collections.emptyList());
        }
    }
}
