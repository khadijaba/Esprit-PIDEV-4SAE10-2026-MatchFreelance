package esprit.skill.Repositories;


import esprit.skill.entities.CV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CVRepository extends JpaRepository<CV, Long> {

    // Récupérer le CV d’un freelance
    Optional<CV> findByFreelancerId(Long freelancerId);

    // Vérifier si un CV existe pour ce freelance
    boolean existsByFreelancerId(Long freelancerId);

    // Supprimer le CV d’un freelance
    void deleteByFreelancerId(Long freelancerId);
}

