package esprit.skill.Repositories;


import esprit.skill.entities.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    // Récupérer tous les skills d’un freelance
    List<Skill> findByFreelancerId(Long freelancerId);

    // Supprimer tous les skills d’un freelance
    void deleteByFreelancerId(Long freelancerId);

    // Rechercher les skills par nom
    List<Skill> findByNameContainingIgnoreCase(String name);
}

