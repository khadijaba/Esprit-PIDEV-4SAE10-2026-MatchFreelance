package esprit.skill.Repositories;

import esprit.skill.entities.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);
}
