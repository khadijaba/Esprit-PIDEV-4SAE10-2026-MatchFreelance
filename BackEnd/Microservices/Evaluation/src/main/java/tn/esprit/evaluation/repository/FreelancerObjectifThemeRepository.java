package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.evaluation.entity.FreelancerObjectifTheme;

import java.util.List;

public interface FreelancerObjectifThemeRepository extends JpaRepository<FreelancerObjectifTheme, Long> {

    List<FreelancerObjectifTheme> findByFreelancerIdAndActifTrueOrderByCreatedAtDesc(Long freelancerId);
}
