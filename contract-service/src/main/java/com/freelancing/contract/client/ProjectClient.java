package com.freelancing.contract.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "project-service", path = "/api/projects")
public interface ProjectClient {

    @GetMapping("/{id}")
    ProjectResponse getProjectById(@PathVariable("id") Long id);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ProjectResponse {
        private Long id;
        private String title;
        private String description;
        private Double minBudget;
        private Double maxBudget;
        private Integer duration;
        private Long clientId;
        private String clientName;
        private String status;
    }
}
