package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.evaluation.entity.ExamenAdaptatifSession;

import java.util.List;
import java.util.Optional;

public interface ExamenAdaptatifSessionRepository extends JpaRepository<ExamenAdaptatifSession, Long> {

    @Query("SELECT DISTINCT s FROM ExamenAdaptatifSession s LEFT JOIN FETCH s.etapes WHERE s.token = :token AND s.examenId = :examenId")
    Optional<ExamenAdaptatifSession> findByTokenAndExamenIdWithEtapes(@Param("token") String token, @Param("examenId") Long examenId);

    Optional<ExamenAdaptatifSession> findByTokenAndExamenId(String token, Long examenId);

    List<ExamenAdaptatifSession> findByExamenIdAndFreelancerIdAndStatut(
            Long examenId, Long freelancerId, ExamenAdaptatifSession.StatutSession statut);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ExamenAdaptatifSession s SET s.statut = :nouveau WHERE s.examenId = :eid AND s.freelancerId = :fid AND s.statut = :ancien")
    int annulerSessionsEnCours(
            @Param("eid") Long examenId,
            @Param("fid") Long freelancerId,
            @Param("ancien") ExamenAdaptatifSession.StatutSession ancien,
            @Param("nouveau") ExamenAdaptatifSession.StatutSession nouveau);
}
