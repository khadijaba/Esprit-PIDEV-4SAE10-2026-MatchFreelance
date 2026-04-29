package esprit.project.Service;

import esprit.project.Repositories.ProjectRepository;
import esprit.project.dto.ProjectEffortEstimateDto;
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
class ProjectEffortEstimateServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectEffortEstimateService service;

    @Test
    void estimateReturnsComputedRangeAndFlags() {
        Project p = new Project();
        p.setId(7L);
        p.setTitle("Migration microservices et sécurité");
        p.setDescription("Refonte architecture microservices avec pipeline CI/CD et exigences de sécurité.");
        p.setDuration(10);
        p.setStatus(ProjectStatus.OPEN);
        p.setRequiredSkills(List.of("Java", "Spring", "DevOps"));
        when(projectRepository.findById(7L)).thenReturn(Optional.of(p));

        ProjectEffortEstimateDto out = service.estimate(7L);

        assertThat(out.getProjectId()).isEqualTo(7L);
        assertThat(out.getEstimatedManDays()).isGreaterThan(0);
        assertThat(out.getEstimatedManDaysLow()).isLessThanOrEqualTo(out.getEstimatedManDays());
        assertThat(out.getEstimatedManDaysHigh()).isGreaterThanOrEqualTo(out.getEstimatedManDays());
        assertThat(out.getMethodVersion()).isEqualTo(ProjectEffortEstimateService.METHOD_VERSION);
        assertThat(out.getSummary()).contains("Charge indicative");
    }

    @Test
    void estimateThrowsNotFoundWhenProjectMissing() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.estimate(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }
}
