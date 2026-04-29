package esprit.project.Service;

import esprit.project.Repositories.ProjectRepository;
import esprit.project.dto.ProjectMlRiskDto;
import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMlRiskServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectMlRiskService service;

    @Test
    void evaluate_usesHeuristicFallback_whenOnnxSessionNotInitialized() {
        Project project = new Project();
        project.setId(15L);
        project.setTitle("AI platform");
        project.setDescription("short desc");
        project.setRequiredSkills(List.of("Java"));
        project.setBudget(300.0);
        project.setDuration(30);
        project.setStatus(ProjectStatus.CANCELLED);
        project.setProjectOwnerId(9L);

        when(projectRepository.findById(15L)).thenReturn(Optional.of(project));
        when(projectRepository.countByProjectOwnerId(9L)).thenReturn(4L);
        when(projectRepository.countByProjectOwnerIdAndStatus(9L, ProjectStatus.COMPLETED)).thenReturn(1L);

        ProjectMlRiskDto risk = service.evaluate(15L);

        assertThat(risk.isHeuristicFallback()).isTrue();
        assertThat(risk.getModelId()).isEqualTo("heuristic-fallback");
        assertThat(risk.getFlags()).contains("PROJET_ANNULE");
        assertThat(risk.getRiskScore0To100()).isBetween(0, 100);
    }

    @Test
    void evaluate_throwsNotFound_whenProjectDoesNotExist() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluate(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project not found");
    }
}
