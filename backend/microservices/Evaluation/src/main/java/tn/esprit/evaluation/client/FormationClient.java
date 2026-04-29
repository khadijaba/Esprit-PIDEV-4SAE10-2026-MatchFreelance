package tn.esprit.evaluation.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    @Value("${evaluation.formation.url:http://localhost:8096}")
    private String formationDirectUrl;

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
                json = fetchDirect("/api/modules/formation/" + formationId);
                if (json == null || json.isBlank()) {
                    return Collections.emptyList();
                }
            }
            List<Map<String, Object>> data = objectMapper.readValue(
                    json,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            return data != null ? data : Collections.emptyList();
        } catch (FeignException e) {
            log.warn("Formation MS modules formation {} -> HTTP {} : {}", formationId, e.status(), e.getMessage());
            try {
                String json = fetchDirect("/api/modules/formation/" + formationId);
                if (json == null || json.isBlank()) return Collections.emptyList();
                List<Map<String, Object>> data = objectMapper.readValue(
                        json,
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return data != null ? data : Collections.emptyList();
            } catch (Exception ignored) {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.warn("Formation MS modules formation {} -> parsing JSON impossible : {}", formationId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getFormationById(Long formationId) {
        try {
            String json = feignApi.getFormationById(formationId);
            if (json == null || json.isBlank()) {
                json = fetchDirect("/api/formations/" + formationId);
                if (json == null || json.isBlank()) {
                    return Collections.emptyMap();
                }
            }
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return data != null ? data : Collections.emptyMap();
        } catch (FeignException e) {
            log.warn("Formation MS getById {} -> HTTP {} : {}", formationId, e.status(), e.getMessage());
            try {
                String json = fetchDirect("/api/formations/" + formationId);
                if (json == null || json.isBlank()) return Collections.emptyMap();
                Map<String, Object> data = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                return data != null ? data : Collections.emptyMap();
            } catch (Exception ignored) {
                return Collections.emptyMap();
            }
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

    private String fetchDirect(String path) {
        try {
            String base = formationDirectUrl != null ? formationDirectUrl.trim() : "";
            if (base.isBlank()) return null;
            String url = (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path;
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            log.warn("Formation direct {} -> HTTP {}", path, response.statusCode());
            return null;
        } catch (Exception e) {
            log.warn("Formation direct {} failed: {}", path, e.getMessage());
            return null;
        }
    }
}

