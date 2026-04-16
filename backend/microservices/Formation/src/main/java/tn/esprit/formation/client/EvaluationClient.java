package tn.esprit.formation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client pour appeler le microservice Evaluation via OpenFeign (service discovery Eureka).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluationClient {

    private final EvaluationFeignApi feignApi;

    /**
     * Récupère les certificats du freelancer (chaque élément contient au moins "examenId").
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCertificatsByFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> data = feignApi.getCertificatsByFreelancer(freelancerId);
            return data != null ? data : Collections.emptyList();
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
            Map<String, Object> examen = feignApi.getExamenById(examenId);
            if (examen != null && examen.get("titre") != null)
                return (String) examen.get("titre");
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'examen {}: {}", examenId, e.getMessage());
        }
        return "examen #" + examenId;
    }
}
