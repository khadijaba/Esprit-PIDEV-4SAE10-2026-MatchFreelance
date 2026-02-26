package com.freelancing.candidature.repository;

import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.enums.CandidatureStatus;
import org.springframework.data.jpa.domain.Specification;

public final class CandidatureSpecifications {
    private CandidatureSpecifications() {}

    public static Specification<Candidature> hasProjectId(Long projectId) {
        return (root, query, cb) -> projectId == null ? null : cb.equal(root.get("projectId"), projectId);
    }

    public static Specification<Candidature> hasFreelancerId(Long freelancerId) {
        return (root, query, cb) -> freelancerId == null ? null : cb.equal(root.get("freelancerId"), freelancerId);
    }

    public static Specification<Candidature> hasStatus(CandidatureStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}

