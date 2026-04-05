package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.evaluation.entity.PassageExamen;

import java.util.List;
import java.util.Optional;

public interface PassageExamenRepository extends JpaRepository<PassageExamen, Long> {

    List<PassageExamen> findByFreelancerIdOrderByDatePassageDesc(Long freelancerId);

    /** Charge les passages avec l'examen en une requête (évite LazyInitializationException). */
    @Query("SELECT DISTINCT p FROM PassageExamen p JOIN FETCH p.examen WHERE p.freelancerId = :freelancerId ORDER BY p.datePassage DESC")
    List<PassageExamen> findByFreelancerIdWithExamen(@Param("freelancerId") Long freelancerId);

    List<PassageExamen> findByExamenIdOrderByDatePassageDesc(Long examenId);

    @Query("SELECT DISTINCT p FROM PassageExamen p JOIN FETCH p.examen WHERE p.examen.id = :examenId ORDER BY p.datePassage DESC")
    List<PassageExamen> findByExamenIdWithExamen(@Param("examenId") Long examenId);

    Optional<PassageExamen> findByExamenIdAndFreelancerId(Long examenId, Long freelancerId);

    @Query("SELECT p FROM PassageExamen p JOIN FETCH p.examen WHERE p.examen.id = :examenId AND p.freelancerId = :freelancerId")
    Optional<PassageExamen> findByExamenIdAndFreelancerIdWithExamen(@Param("examenId") Long examenId, @Param("freelancerId") Long freelancerId);

    boolean existsByExamenIdAndFreelancerId(Long examenId, Long freelancerId);
}
