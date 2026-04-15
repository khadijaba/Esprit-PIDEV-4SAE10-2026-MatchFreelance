package tn.esprit.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.evaluation.entity.FreelancerAmenagementTemps;

import java.util.Optional;

public interface FreelancerAmenagementTempsRepository extends JpaRepository<FreelancerAmenagementTemps, Long> {

    Optional<FreelancerAmenagementTemps> findByFreelancerId(Long freelancerId);
}
