package com.freelancing.interview.repository;

import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.enums.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByCandidatureId(Long candidatureId);

    long countByCandidatureId(Long candidatureId);

    boolean existsByCandidatureIdAndStatusIn(Long candidatureId, Collection<InterviewStatus> statuses);
}
