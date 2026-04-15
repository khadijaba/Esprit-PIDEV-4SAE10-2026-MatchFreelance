package com.freelancing.candidature.client;

import com.freelancing.candidature.client.dto.ProjectRemotePayload;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectClient {

    private final ProjectRemoteFeign projectRemoteFeign;

    public ProjectResponse getProjectById(Long id) {
        if (id == null) {
            return null;
        }
        try {
            ProjectRemotePayload p = projectRemoteFeign.getById(id);
            return map(p);
        } catch (Exception e) {
            return null;
        }
    }

    public void updateProjectStatus(Long id, ProjectUpdateRequest request) {
        ProjectRemotePayload current = projectRemoteFeign.getById(id);
        if (current == null) {
            return;
        }
        if (request.getTitle() != null) {
            current.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            current.setDescription(request.getDescription());
        }
        if (request.getMinBudget() != null) {
            current.setBudget(request.getMinBudget());
        }
        if (request.getMaxBudget() != null && request.getMinBudget() == null) {
            current.setBudget(request.getMaxBudget());
        }
        if (request.getDuration() != null) {
            current.setDuration(request.getDuration());
        }
        if (request.getStatus() != null) {
            current.setStatus(request.getStatus());
        }
        if (request.getClientId() != null) {
            current.setProjectOwnerId(request.getClientId());
        }
        projectRemoteFeign.update(id, current);
    }

    private static ProjectResponse map(ProjectRemotePayload p) {
        if (p == null) {
            return null;
        }
        ProjectResponse r = new ProjectResponse();
        r.setId(p.getId());
        r.setTitle(p.getTitle());
        r.setDescription(p.getDescription());
        Double b = p.getBudget();
        r.setMinBudget(b);
        r.setMaxBudget(b);
        r.setDuration(p.getDuration());
        r.setStatus(p.getStatus());
        r.setClientId(p.getProjectOwnerId());
        return r;
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
