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
 * Appels au microservice Project via Eureka ({@code http://PROJECT/...}).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectClient {

    private static final String PROJECT = "http://PROJECT";
    private final RestTemplate restTemplate;

    public List<Map<String, Object>> getProjectsByStatus(String status) {
        try {
            List<Map<String, Object>> body = restTemplate.exchange(
                    PROJECT + "/projects/status/" + status,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Project MS statut {}: {}", status, e.getMessage());
            return Collections.emptyList();
        }
    }
}
