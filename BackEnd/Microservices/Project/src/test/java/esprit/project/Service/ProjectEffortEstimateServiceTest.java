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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectEffortEstimateServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectEffortEstimateService service;

    @Test
    void estimate_returnsHeuristicVersionAndFteRatio() {
        long id = 42L;
        Project p = new Project();
        p.setId(id);
        p.setTitle("Application web de gestion des missions");
        p.setDescription(
                "Lorem ipsum ".repeat(20).trim()
                        + " microservices kubernetes ci/cd livrables recette critères d'acceptation."
        );
        p.setBudget(10_000.0);
        p.setDuration(60);
        p.setStatus(ProjectStatus.OPEN);
        p.setProjectOwnerId(1L);
        p.setRequiredSkills(List.of("Java", "Angular"));

        when(projectRepository.findById(id)).thenReturn(Optional.of(p));

        ProjectEffortEstimateDto dto = service.estimate(id);

        assertThat(dto.getMethodVersion()).isEqualTo(ProjectEffortEstimateService.METHOD_VERSION);
        assertThat(dto.getEstimatedManDays()).isBetween(4, 320);
        assertThat(dto.getDeclaredDurationDays()).isEqualTo(60);
        assertThat(dto.getEstimatedManDaysLow()).isLessThanOrEqualTo(dto.getEstimatedManDays());
        assertThat(dto.getEstimatedManDaysHigh()).isGreaterThanOrEqualTo(dto.getEstimatedManDays());
        double expectedFte = dto.getEstimatedManDays() / 60.0;
        assertThat(dto.getFteRequiredVsDeclared()).isEqualTo(Math.round(expectedFte * 100.0) / 100.0);
        if (expectedFte > 1.25) {
            assertThat(dto.getFlags()).contains("EFFORT_SUPERIEUR_A_UNE_RESSOURCE_SUR_LA_DUREE");
        }
    }

    @Test
    void estimate_emptySkills_addsFlag() {
        long id = 7L;
        Project p = new Project();
        p.setId(id);
        p.setTitle("Titre assez long pour le test");
        p.setDescription(
                "Une description avec suffisamment de mots pour dépasser le seuil de quarante "
                        + "mots et éviter le drapeau DESCRIPTION_LEGERE_CHARGE_INCERTAINE dans ce scénario."
        );
        p.setBudget(5_000.0);
        p.setDuration(30);
        p.setStatus(ProjectStatus.OPEN);
        p.setProjectOwnerId(2L);
        p.setRequiredSkills(List.of());

        when(projectRepository.findById(id)).thenReturn(Optional.of(p));

        ProjectEffortEstimateDto dto = service.estimate(id);

        assertThat(dto.getFlags()).contains("PAS_DE_COMPETENCES_POUR_AFFINER");
    }
}
