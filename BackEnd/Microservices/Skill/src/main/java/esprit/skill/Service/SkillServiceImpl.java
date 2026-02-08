package esprit.skill.Service;


import esprit.skill.Repositories.SkillRepository;
import esprit.skill.entities.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillServiceImpl implements SkillService {

    @Autowired
    private SkillRepository skillRepository;

    @Override
    public Skill addSkill(Skill skill) {
        return skillRepository.save(skill);
    }

    @Override
    public List<Skill> getAllSkills() {
        return skillRepository.findAll();
    }

    @Override
    public List<Skill> getSkillsByFreelancer(Long freelancerId) {
        return skillRepository.findByFreelancerId(freelancerId);
    }

    @Override
    public Skill updateSkill(Long id, Skill updatedSkill) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found"));

        skill.setName(updatedSkill.getName());
        skill.setLevel(updatedSkill.getLevel());

        return skillRepository.save(skill);
    }

    @Override
    public void deleteSkill(Long id) {
        skillRepository.deleteById(id);
    }

    @Override
    public void deleteSkillsByFreelancer(Long freelancerId) {
        skillRepository.deleteByFreelancerId(freelancerId);
    }
}
