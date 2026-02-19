package esprit.project.Controllers;

import esprit.project.Service.ProjectService;
import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return projectService.createProject(project);
    }

    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @GetMapping("/status/{status}")
    public List<Project> getProjectsByStatus(@PathVariable ProjectStatus status) {
        return projectService.getProjectsByStatus(status);
    }

    @GetMapping("/owner/{projectOwnerId}")
    public List<Project> getProjectsByOwnerId(@PathVariable Long projectOwnerId) {
        return projectService.getProjectsByOwnerId(projectOwnerId);
    }

    @GetMapping("/search")
    public List<Project> searchProjectsByTitle(@RequestParam String title) {
        return projectService.searchProjectsByTitle(title);
    }

    @GetMapping("/skill/{skill}")
    public List<Project> getProjectsByRequiredSkill(@PathVariable String skill) {
        return projectService.getProjectsByRequiredSkill(skill);
    }

    @PutMapping("/{id}")
    public Project updateProject(@PathVariable Long id, @RequestBody Project project) {
        return projectService.updateProject(id, project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }
}
