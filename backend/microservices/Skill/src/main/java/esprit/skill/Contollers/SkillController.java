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

    /** Endpoint de diagnostic : vérifier que la Gateway route bien vers SKILL. */
    @GetMapping("/ping")
    public java.util.Map<String, String> ping() {
        return java.util.Map.of("status", "UP", "service", "SKILL");
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
    public Skill getSkillById(@PathVariable Long id) {
        return skillService.getSkillById(id);
    }

    @GetMapping("/freelancer/{freelancerId}")
    public List<Skill> getSkillsByFreelancer(@PathVariable Long freelancerId) {
        return skillService.getSkillsByFreelancer(freelancerId);
    }

    @GetMapping("/category/{category}")
    public List<Skill> getSkillsByCategory(@PathVariable SkillCategory category) {
        return skillService.getSkillsByCategory(category);
    }

    @PutMapping("/{id}")
    public Skill updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        return skillService.updateSkill(id, skill);
    }

    @DeleteMapping("/{id}")
    public void deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
    }
}
