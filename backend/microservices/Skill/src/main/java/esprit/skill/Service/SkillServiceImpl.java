package esprit.skill.Service;

import esprit.skill.Repositories.SkillRepository;
import esprit.skill.entities.Skill;
import esprit.skill.entities.SkillCategory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;

    public SkillServiceImpl(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Override
    public Skill addSkill(Skill skill) {
        if (skillRepository.existsByFreelancerIdAndName(skill.getFreelancerId(), skill.getName())) {
            throw new RuntimeException("Skill already exists for this freelancer");
        }
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
    public List<Skill> getSkillsByCategory(SkillCategory category) {
        return skillRepository.findByCategory(category);
    }

    @Override
    public Skill getSkillById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
    }

    @Override
    public Skill updateSkill(Long id, Skill updatedSkill) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
        skill.setName(updatedSkill.getName());
        skill.setCategory(updatedSkill.getCategory());
        skill.setLevel(updatedSkill.getLevel());
        skill.setYearsOfExperience(updatedSkill.getYearsOfExperience());
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
