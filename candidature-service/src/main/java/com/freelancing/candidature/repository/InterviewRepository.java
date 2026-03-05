package com.freelancing.candidature.repository;

import com.freelancing.candidature.entity.Interview;
import com.freelancing.candidature.enums.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByCandidatureId(Long candidatureId);

    List<Interview> findByCandidatureIdAndStatus(Long candidatureId, InterviewStatus status);
}
