package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.evaluation.entity.Examen;

import java.util.List;
import java.util.Optional;

public interface ExamenRepository extends JpaRepository<Examen, Long> {

    List<Examen> findByFormationIdOrderByIdAsc(Long formationId);

    Optional<Examen> findByFormationId(Long formationId);
}
