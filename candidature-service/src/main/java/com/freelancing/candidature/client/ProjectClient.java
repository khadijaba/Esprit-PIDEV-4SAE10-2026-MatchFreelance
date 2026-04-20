package com.freelancing.candidature.client;

import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProjectClient {

    private static final String SERVICE_URL = "http://project-service";

    private final RestTemplate restTemplate;

    public ProjectClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ProjectResponse getProjectById(Long id) {
        try {
            return restTemplate.getForObject(SERVICE_URL + "/api/projects/" + id, ProjectResponse.class);
        } catch (RestClientException e) {
            return null;
        }
    }

    public void updateProjectStatus(Long id, ProjectUpdateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProjectUpdateRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.put(SERVICE_URL + "/api/projects/" + id, entity);
    }

    @Data
    public static class ProjectResponse {
        private Long id;
        private String title;
        private String description;
        private Double minBudget;
        private Double maxBudget;
        private Integer duration;
        private String status;
        private Long clientId;
    }

    @Data
    public static class ProjectUpdateRequest {
        private String title;
        private String description;
        private Double minBudget;
        private Double maxBudget;
        private Integer duration;
        private String status;
        private Long clientId;
    }
}
