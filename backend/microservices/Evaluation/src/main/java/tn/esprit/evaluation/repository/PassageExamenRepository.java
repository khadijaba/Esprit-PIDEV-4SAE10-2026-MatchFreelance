package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.evaluation.entity.PassageExamen;

import java.util.List;
import java.util.Optional;

public interface PassageExamenRepository extends JpaRepository<PassageExamen, Long> {

    List<PassageExamen> findByFreelancerIdOrderByDatePassageDesc(Long freelancerId);

    List<PassageExamen> findByExamenIdOrderByDatePassageDesc(Long examenId);

    Optional<PassageExamen> findByExamenIdAndFreelancerId(Long examenId, Long freelancerId);

    boolean existsByExamenIdAndFreelancerId(Long examenId, Long freelancerId);
}
