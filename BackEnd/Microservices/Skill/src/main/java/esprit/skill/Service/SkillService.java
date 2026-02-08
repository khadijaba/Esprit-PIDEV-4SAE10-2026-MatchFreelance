package esprit.skill.Service;

import esprit.skill.entities.Skill;

import java.util.List;

public interface SkillService {

    Skill addSkill(Skill skill);

    List<Skill> getAllSkills();

    List<Skill> getSkillsByFreelancer(Long freelancerId);

    Skill updateSkill(Long id, Skill skill);

    void deleteSkill(Long id);

    void deleteSkillsByFreelancer(Long freelancerId);
}
