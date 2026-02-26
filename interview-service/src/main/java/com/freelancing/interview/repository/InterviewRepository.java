package com.freelancing.interview.repository;

import com.freelancing.interview.entity.Interview;
import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long>, JpaSpecificationExecutor<Interview> {
    Page<Interview> findByFreelancerId(Long freelancerId, Pageable pageable);

    List<Interview> findByStatusAndStartAtBetween(InterviewStatus status, Instant from, Instant to);

    Page<Interview> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Interview> findByStatus(InterviewStatus status, Pageable pageable);

    Page<Interview> findByMode(MeetingMode mode, Pageable pageable);

    Page<Interview> findByStartAtBetween(Instant from, Instant to, Pageable pageable);

    @Query("""
            select (count(i) > 0) from Interview i
            where i.freelancerId = :freelancerId
              and i.status in :blockingStatuses
              and i.startAt < :endAt
              and i.endAt > :startAt
              and (:excludeId is null or i.id <> :excludeId)
            """)
    boolean existsOverlappingForFreelancer(
            @Param("freelancerId") Long freelancerId,
            @Param("blockingStatuses") List<InterviewStatus> blockingStatuses,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludeId") Long excludeId
    );

    @Query("""
            select (count(i) > 0) from Interview i
            where i.ownerId = :ownerId
              and i.status in :blockingStatuses
              and i.startAt < :endAt
              and i.endAt > :startAt
              and (:excludeId is null or i.id <> :excludeId)
            """)
    boolean existsOverlappingForOwner(
            @Param("ownerId") Long ownerId,
            @Param("blockingStatuses") List<InterviewStatus> blockingStatuses,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludeId") Long excludeId
    );
}

