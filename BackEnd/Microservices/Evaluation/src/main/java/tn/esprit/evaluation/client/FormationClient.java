package tn.esprit.evaluation.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Client vers le microservice Formation via OpenFeign (service discovery Eureka).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FormationClient {

    private final FormationFeignApi feignApi;
    private final ObjectMapper objectMapper;

    public List<Map<String, Object>> getRecommandationsForFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> data = feignApi.getRecommandationsForFreelancer(freelancerId);
            return data != null ? data : Collections.emptyList();
        } catch (FeignException e) {
            log.warn("Formation MS recommandations freelancer {} -> HTTP {} : {}", freelancerId, e.status(), e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Impossible de récupérer les formations recommandées pour freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getModulesByFormation(Long formationId) {
        try {
            String json = feignApi.getModulesByFormation(formationId);
            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> data = objectMapper.readValue(
                    json,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            return data != null ? data : Collections.emptyList();
        } catch (FeignException e) {
            log.warn("Formation MS modules formation {} -> HTTP {} : {}", formationId, e.status(), e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Formation MS modules formation {} -> parsing JSON impossible : {}", formationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getFormationById(Long formationId) {
        try {
            String json = feignApi.getFormationById(formationId);
            if (json == null || json.isBlank()) {
                return Collections.emptyMap();
            }
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return data != null ? data : Collections.emptyMap();
        } catch (FeignException e) {
            log.warn("Formation MS getById {} -> HTTP {} : {}", formationId, e.status(), e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Formation MS getById {} -> parsing JSON impossible : {}", formationId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    public List<Map<String, Object>> getInscriptionsByFreelancer(Long freelancerId) {
        try {
            List<Map<String, Object>> data = feignApi.getInscriptionsByFreelancer(freelancerId);
            return data != null ? data : Collections.emptyList();
        } catch (FeignException e) {
            log.warn("Formation MS inscriptions freelancer {} -> HTTP {} : {}", freelancerId, e.status(), e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Impossible de récupérer les inscriptions freelancer {}: {}", freelancerId, e.getMessage());
            return Collections.emptyList();
        }
    }
}

