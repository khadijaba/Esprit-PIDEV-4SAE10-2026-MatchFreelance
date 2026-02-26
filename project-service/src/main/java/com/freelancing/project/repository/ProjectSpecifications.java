package com.freelancing.project.repository;

import com.freelancing.project.entity.Project;
import com.freelancing.project.enums.ProjectStatus;
import org.springframework.data.jpa.domain.Specification;

public final class ProjectSpecifications {
    private ProjectSpecifications() {}

    public static Specification<Project> hasStatus(ProjectStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Project> hasClientId(Long clientId) {
        return (root, query, cb) -> clientId == null ? null : cb.equal(root.get("clientId"), clientId);
    }

    public static Specification<Project> titleContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            String like = "%" + q.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("title")), like);
        };
    }
}

