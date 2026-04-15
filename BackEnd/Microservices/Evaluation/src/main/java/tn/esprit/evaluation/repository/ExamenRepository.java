package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.evaluation.entity.Examen;

import java.util.List;
import java.util.Optional;

public interface ExamenRepository extends JpaRepository<Examen, Long> {

    List<Examen> findByFormationIdOrderByIdAsc(Long formationId);

    /** Charge les examens et leurs questions en une requête (évite LazyInitializationException). */
    @Query("SELECT DISTINCT e FROM Examen e LEFT JOIN FETCH e.questions WHERE e.formationId = :formationId ORDER BY e.id")
    List<Examen> findByFormationIdWithQuestions(@Param("formationId") Long formationId);

    /** Charge un examen avec ses questions (pour getById / getPourPassage). */
    @Query("SELECT e FROM Examen e LEFT JOIN FETCH e.questions WHERE e.id = :id")
    Optional<Examen> findByIdWithQuestions(@Param("id") Long id);

    Optional<Examen> findByFormationId(Long formationId);
}
