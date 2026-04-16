package tn.esprit.evaluation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Appels au microservice Skill via Eureka ({@code http://SKILL/...}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SkillClient {

    private static final String SKILL = "http://SKILL";
    private final RestTemplate restTemplate;

    public List<Map<String, Object>> getSkillsByFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> body = restTemplate.exchange(
                    SKILL + "/skills/freelancer/" + freelancerId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Skill MS indisponible pour freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /** Crée une compétence (POST /skills). Retourne l'id créé ou null. */
    public Long createSkill(Map<String, Object> skillPayload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = restTemplate.exchange(
                    SKILL + "/skills",
                    HttpMethod.POST,
                    new HttpEntity<>(skillPayload, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();
            if (body == null || body.get("id") == null) {
                return null;
            }
            Object id = body.get("id");
            if (id instanceof Number n) {
                return n.longValue();
            }
            return Long.parseLong(id.toString());
        } catch (Exception e) {
            log.warn("Création skill échouée: {}", e.getMessage());
            return null;
        }
    }
}
