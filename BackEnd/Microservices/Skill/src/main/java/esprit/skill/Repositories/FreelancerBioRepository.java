package esprit.skill.Repositories;

import esprit.skill.entities.FreelancerBio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreelancerBioRepository extends JpaRepository<FreelancerBio, Long> {

    Optional<FreelancerBio> findByFreelancerId(Long freelancerId);

    void deleteByFreelancerId(Long freelancerId);
}
