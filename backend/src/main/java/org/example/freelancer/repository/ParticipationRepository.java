package org.example.freelancer.repository;

import org.example.freelancer.entity.Participation;
import org.example.freelancer.entity.ParticipationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ParticipationRepository extends JpaRepository<Participation, ParticipationId> {

    List<Participation> findByEventId(Long eventId);

    List<Participation> findByUserId(Long userId);

    long countByEventId(Long eventId);

    @Query("SELECT p FROM Participation p WHERE p.eventId = :eventId AND p.score IS NOT NULL ORDER BY p.score DESC")
    List<Participation> findLeaderboard(@Param("eventId") Long eventId);
}
