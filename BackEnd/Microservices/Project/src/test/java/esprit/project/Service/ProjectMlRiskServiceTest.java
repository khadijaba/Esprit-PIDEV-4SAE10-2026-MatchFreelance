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
    void evaluate_throwsNotFound_whenProjectDoesNotExist() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluate(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void evaluate_usesHeuristicFallback_whenOnnxIsUnavailable() {
        Project p = newProject(1L, ProjectStatus.OPEN, "court", "peu de mots", List.of());
        p.setProjectOwnerId(77L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepository.countByProjectOwnerId(77L)).thenReturn(2L);
        when(projectRepository.countByProjectOwnerIdAndStatus(77L, ProjectStatus.COMPLETED)).thenReturn(0L);

        ProjectMlRiskDto out = service.evaluate(1L);

        assertThat(out.isHeuristicFallback()).isTrue();
        assertThat(out.getModelId()).isEqualTo("heuristic-fallback");
        assertThat(out.getRiskLevel()).isIn("LOW", "MEDIUM", "HIGH");
        assertThat(out.getRiskScore0To100()).isBetween(0, 100);
        assertThat(out.getFlags()).isNotNull();
    }

    @Test
    void evaluate_increasesRiskForCancelledProject() {
        Project cancelled = newProject(2L, ProjectStatus.CANCELLED, "titre", "desc", List.of("java"));
        cancelled.setProjectOwnerId(88L);

        when(projectRepository.findById(2L)).thenReturn(Optional.of(cancelled));
        when(projectRepository.countByProjectOwnerId(88L)).thenReturn(1L);
        when(projectRepository.countByProjectOwnerIdAndStatus(88L, ProjectStatus.COMPLETED)).thenReturn(0L);

        ProjectMlRiskDto out = service.evaluate(2L);

        assertThat(out.getFlags()).contains("PROJET_ANNULE");
        assertThat(out.getProbabilityHighRisk()).isGreaterThan(0.3f);
        assertThat(out.getSummary()).contains("Repli heuristique");
    }

    private static Project newProject(Long id, ProjectStatus status, String title, String description, List<String> skills) {
        Project p = new Project();
        p.setId(id);
        p.setTitle(title);
        p.setDescription(description);
        p.setBudget(1000.0);
        p.setDuration(30);
        p.setStatus(status);
        p.setRequiredSkills(skills);
        return p;
    }
}
