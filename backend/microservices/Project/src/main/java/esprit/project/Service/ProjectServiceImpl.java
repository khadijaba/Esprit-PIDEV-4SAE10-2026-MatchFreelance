package esprit.project.Service;

import esprit.project.Repositories.ProjectRepository;
import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public Project createProject(Project project) {
        if (project.getStatus() == null) {
            project.setStatus(ProjectStatus.OPEN);
        }
        return projectRepository.save(project);
    }

    @Override
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Override
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    @Override
    public List<Project> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status);
    }

    @Override
    public List<Project> getProjectsByOwnerId(Long projectOwnerId) {
        return projectRepository.findByProjectOwnerId(projectOwnerId);
    }

    @Override
    public List<Project> searchProjectsByTitle(String title) {
        return projectRepository.findByTitleContaining(title);
    }

    @Override
    public List<Project> getProjectsByRequiredSkill(String skill) {
        return projectRepository.findByRequiredSkillsContaining(skill);
    }

    @Override
    public Project updateProject(Long id, Project updatedProject) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        project.setTitle(updatedProject.getTitle());
        project.setDescription(updatedProject.getDescription());
        project.setBudget(updatedProject.getBudget());
        project.setDuration(updatedProject.getDuration());
        if (updatedProject.getStatus() != null) {
            project.setStatus(updatedProject.getStatus());
        }
        if (updatedProject.getRequiredSkills() != null) {
            project.setRequiredSkills(updatedProject.getRequiredSkills());
        }

        return projectRepository.save(project);
    }

    @Override
    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }
}
