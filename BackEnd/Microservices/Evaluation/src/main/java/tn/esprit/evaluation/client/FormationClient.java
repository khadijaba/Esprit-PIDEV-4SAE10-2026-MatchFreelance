package tn.esprit.evaluation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client vers le microservice Formation via Eureka (lb://FORMATION).
 * Utilisé pour renvoyer des formations recommandées après un examen (feedback).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FormationClient {

    private static final String FORMATION_SERVICE = "http://FORMATION";
    private final RestTemplate restTemplate;

    public List<Map<String, Object>> getRecommandationsForFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> data = restTemplate.exchange(
                    FORMATION_SERVICE + "/api/formations/recommandations/freelancer/" + freelancerId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return data != null ? data : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Impossible de récupérer les formations recommandées pour freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getModulesByFormation(Long formationId) {
        try {
            List<Map<String, Object>> data = restTemplate.exchange(
                    FORMATION_SERVICE + "/api/modules/formation/" + formationId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return data != null ? data : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Impossible de récupérer les modules formation {}: {}", formationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getFormationById(Long formationId) {
        try {
            Map<String, Object> data = restTemplate.exchange(
                    FORMATION_SERVICE + "/api/formations/" + formationId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();
            return data != null ? data : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Impossible de récupérer la formation {}: {}", formationId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    public List<Map<String, Object>> getInscriptionsByFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> data = restTemplate.exchange(
                    FORMATION_SERVICE + "/api/inscriptions/freelancer/" + freelancerId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return data != null ? data : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Impossible de récupérer les inscriptions freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }
}

