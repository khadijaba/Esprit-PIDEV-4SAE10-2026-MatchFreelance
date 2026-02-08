package esprit.candidature.Ripository;


import esprit.candidature.entities.Candidature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    List<Candidature> findByFreelancerId(Long freelancerId);

    List<Candidature> findByProjectId(Long projectId);
}
