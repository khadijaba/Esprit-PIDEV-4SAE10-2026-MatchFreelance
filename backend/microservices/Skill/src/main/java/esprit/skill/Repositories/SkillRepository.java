package esprit.skill.Repositories;

import esprit.skill.entities.Skill;
import esprit.skill.entities.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByFreelancerId(Long freelancerId);

    List<Skill> findByCategory(SkillCategory category);

    List<Skill> findByFreelancerIdAndCategory(Long freelancerId, SkillCategory category);

    boolean existsByFreelancerIdAndName(Long freelancerId, String name);

    void deleteByFreelancerId(Long freelancerId);

    List<Skill> findByNameContainingIgnoreCase(String name);
}
