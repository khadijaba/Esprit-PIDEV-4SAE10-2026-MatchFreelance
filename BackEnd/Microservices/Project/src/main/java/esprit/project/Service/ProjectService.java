package esprit.project.Service;

import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;

import java.util.List;

public interface ProjectService {

    Project createProject(Project project);

    List<Project> getAllProjects();

    Project getProjectById(Long id);

    List<Project> getProjectsByStatus(ProjectStatus status);

    List<Project> getProjectsByOwnerId(Long projectOwnerId);

    List<Project> searchProjectsByTitle(String title);

    List<Project> getProjectsByRequiredSkill(String skill);

    Project updateProject(Long id, Project project);

    void deleteProject(Long id);
}
