package com.freelancing.candidature.repository;

import com.freelancing.candidature.entity.Candidature;
import com.freelancing.candidature.enums.CandidatureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long>, JpaSpecificationExecutor<Candidature> {

    List<Candidature> findByProjectId(Long projectId);

    List<Candidature> findByFreelancerId(Long freelancerId);

    boolean existsByProjectIdAndFreelancerId(Long projectId, Long freelancerId);

    List<Candidature> findByProjectIdAndStatus(Long projectId, CandidatureStatus status);

    Page<Candidature> findByProjectId(Long projectId, Pageable pageable);

    Page<Candidature> findByFreelancerId(Long freelancerId, Pageable pageable);
}
