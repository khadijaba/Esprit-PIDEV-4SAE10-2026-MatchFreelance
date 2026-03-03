package esprit.skill.Contollers;

import esprit.skill.Service.SkillService;
import esprit.skill.entities.Skill;
import esprit.skill.entities.SkillCategory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/skills")
@CrossOrigin(origins = "*")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping
    public Skill addSkill(@RequestBody Skill skill) {
        return skillService.addSkill(skill);
    }

    @GetMapping
    public List<Skill> getAllSkills() {
        return skillService.getAllSkills();
    }

    @GetMapping("/{id}")
    public Skill getSkillById(@PathVariable("id") Long id) {
        return skillService.getSkillById(id);
    }

    @GetMapping("/freelancer/{freelancerId}")
    public List<Skill> getSkillsByFreelancer(@PathVariable("freelancerId") Long freelancerId) {
        return skillService.getSkillsByFreelancer(freelancerId);
    }

    @GetMapping("/category/{category}")
    public List<Skill> getSkillsByCategory(@PathVariable("category") SkillCategory category) {
        return skillService.getSkillsByCategory(category);
    }

    @PutMapping("/{id}")
    public Skill updateSkill(@PathVariable("id") Long id, @RequestBody Skill skill) {
        return skillService.updateSkill(id, skill);
    }

    @DeleteMapping("/{id}")
    public void deleteSkill(@PathVariable("id") Long id) {
        skillService.deleteSkill(id);
    }
}

