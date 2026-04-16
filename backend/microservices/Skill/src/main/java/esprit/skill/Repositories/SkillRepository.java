package esprit.skill.Repositories;


import esprit.skill.entities.Skill;
import esprit.skill.entities.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    // Récupérer tous les skills d'un freelance
    List<Skill> findByFreelancerId(Long freelancerId);

    // Récupérer les skills par catégorie
    List<Skill> findByCategory(SkillCategory category);

    // Récupérer les skills d'un freelance par catégorie
    List<Skill> findByFreelancerIdAndCategory(Long freelancerId, SkillCategory category);

    // Vérifier si un skill existe déjà pour un freelance
    boolean existsByFreelancerIdAndName(Long freelancerId, String name);

    // Supprimer tous les skills d'un freelance
    void deleteByFreelancerId(Long freelancerId);

    // Rechercher les skills par nom
    List<Skill> findByNameContainingIgnoreCase(String name);
}
