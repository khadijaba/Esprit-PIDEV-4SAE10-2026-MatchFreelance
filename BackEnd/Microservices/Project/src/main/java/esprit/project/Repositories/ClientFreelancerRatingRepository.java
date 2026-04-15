package esprit.project.Repositories;

import esprit.project.entities.ClientFreelancerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClientFreelancerRatingRepository extends JpaRepository<ClientFreelancerRating, Long> {

    Optional<ClientFreelancerRating> findByProjectIdAndFreelancerId(Long projectId, Long freelancerId);

    @Query("SELECT AVG(r.rating) FROM ClientFreelancerRating r WHERE r.freelancerId = :freelancerId")
    Double averageRatingByFreelancerId(@Param("freelancerId") Long freelancerId);
}
