package com.freelancing.interview.repository;

import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class InterviewSpecifications {
    private InterviewSpecifications() {}

    public static Specification<Interview> hasFreelancerId(Long freelancerId) {
        return (root, query, cb) -> freelancerId == null ? null : cb.equal(root.get("freelancerId"), freelancerId);
    }

    public static Specification<Interview> hasOwnerId(Long ownerId) {
        return (root, query, cb) -> ownerId == null ? null : cb.equal(root.get("ownerId"), ownerId);
    }

    public static Specification<Interview> hasProjectId(Long projectId) {
        return (root, query, cb) -> projectId == null ? null : cb.equal(root.get("projectId"), projectId);
    }

    public static Specification<Interview> hasCandidatureId(Long candidatureId) {
        return (root, query, cb) -> candidatureId == null ? null : cb.equal(root.get("candidatureId"), candidatureId);
    }

    public static Specification<Interview> hasStatus(InterviewStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Interview> hasMode(MeetingMode mode) {
        return (root, query, cb) -> mode == null ? null : cb.equal(root.get("mode"), mode);
    }

    public static Specification<Interview> startAtGte(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("startAt"), from);
    }

    public static Specification<Interview> startAtLte(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("startAt"), to);
    }
}

