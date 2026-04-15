package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.evaluation.entity.FreelancerThemeScoreLog;

import java.util.Optional;

public interface FreelancerThemeScoreLogRepository extends JpaRepository<FreelancerThemeScoreLog, Long> {

    Optional<FreelancerThemeScoreLog> findTopByFreelancerIdAndExamenIdAndThemeOrderByCreatedAtDesc(
            Long freelancerId,
            Long examenId,
            String theme);
}
