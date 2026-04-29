package com.freelancing.candidature.repository;

import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.enums.CandidatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    List<Candidature> findByProjectId(Long projectId);

    List<Candidature> findByFreelancerId(Long freelancerId);

    boolean existsByProjectIdAndFreelancerId(Long projectId, Long freelancerId);

    List<Candidature> findByProjectIdAndStatus(Long projectId, CandidatureStatus status);

    long countByProjectId(Long projectId);
}
