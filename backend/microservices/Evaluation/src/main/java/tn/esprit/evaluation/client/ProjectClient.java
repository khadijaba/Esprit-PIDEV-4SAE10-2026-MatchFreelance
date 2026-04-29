package tn.esprit.evaluation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Appels au microservice Project via OpenFeign.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectClient {

    private final ProjectFeignApi feignApi;

    public List<Map<String, Object>> getProjectsByStatus(String status) {
        try {
            List<Map<String, Object>> body = feignApi.getProjectsByStatus(status);
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Project MS statut {}: {}", status, e.getMessage());
            return Collections.emptyList();
        }
    }
}
