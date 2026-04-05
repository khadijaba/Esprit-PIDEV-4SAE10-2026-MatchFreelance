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

/**
 * Client pour appeler le microservice Evaluation (certificats, examens) via Eureka (lb://EVALUATION).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluationClient {

    private static final String EVALUATION_SERVICE = "http://EVALUATION";
    private final RestTemplate restTemplate;

    /**
     * Récupère les certificats du freelancer (chaque élément contient au moins "examenId").
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCertificatsByFreelancer(Long freelancerId) {
        try {
            return restTemplate.exchange(
                    EVALUATION_SERVICE + "/api/certificats/freelancer/" + freelancerId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (Exception e) {
            log.warn("Impossible de récupérer les certificats du freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Récupère le titre de l'examen (pour le message d'erreur "certificat Y requis").
     */
    public String getExamenTitre(Long examenId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> examen = restTemplate.getForObject(
                    EVALUATION_SERVICE + "/api/examens/" + examenId,
                    Map.class
            );
            if (examen != null && examen.get("titre") != null)
                return (String) examen.get("titre");
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'examen {}: {}", examenId, e.getMessage());
        }
        return "examen #" + examenId;
    }
}
