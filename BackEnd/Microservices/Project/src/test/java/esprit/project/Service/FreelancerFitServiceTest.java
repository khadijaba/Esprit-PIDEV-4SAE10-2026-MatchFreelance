package esprit.project.Service;

import esprit.project.Repositories.ClientFreelancerRatingRepository;
import esprit.project.Repositories.ProjectRepository;
import esprit.project.client.CandidatureClient;
import esprit.project.dto.SubmitFreelancerRatingRequest;
import esprit.project.dto.candidature.CandidatureSummaryDto;
import esprit.project.entities.ClientFreelancerRating;
import esprit.project.entities.Project;
import esprit.project.entities.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreelancerFitServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private CandidatureClient candidatureClient;
    @Mock
    private ClientFreelancerRatingRepository ratingRepository;

    @InjectMocks
    private FreelancerFitService service;

    @Test
    void submitRating_updatesExistingRow_whenRatingAlreadyExists() {
        Project project = newProject(10L);
        ClientFreelancerRating existing = new ClientFreelancerRating();
        existing.setId(99L);
        existing.setProjectId(10L);
        existing.setFreelancerId(7L);
        existing.setRating(2);

        SubmitFreelancerRatingRequest request = new SubmitFreelancerRatingRequest();
        request.setFreelancerId(7L);
        request.setRating(5);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(ratingRepository.findByProjectIdAndFreelancerId(10L, 7L)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(existing)).thenReturn(existing);

        ClientFreelancerRating saved = service.submitRating(10L, request);

        assertThat(saved.getProjectId()).isEqualTo(10L);
        assertThat(saved.getFreelancerId()).isEqualTo(7L);
        assertThat(saved.getRating()).isEqualTo(5);
        verify(ratingRepository).save(existing);
    }

    @Test
    void submitRating_throwsBadRequest_whenMissingRequiredFields() {
        Project project = newProject(5L);
        SubmitFreelancerRatingRequest request = new SubmitFreelancerRatingRequest();
        request.setFreelancerId(null);
        request.setRating(4);

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.submitRating(5L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("freelancerId and rating required");
    }

    @Test
    void computeFit_handlesMixedPastProjects_andReturnsSingleDistinctFreelancer() {
        Project target = newProject(20L);
        target.setRequiredSkills(List.of("Java", "Spring"));
        target.setDuration(30);

        CandidatureSummaryDto accepted = new CandidatureSummaryDto();
        accepted.setProjectId(101L);
        accepted.setStatus("ACCEPTED");
        accepted.setFreelancerId(77L);

        Project completed = newProject(101L);
        completed.setStatus(ProjectStatus.COMPLETED);
        completed.setRequiredSkills(List.of("Spring", "SQL"));
        completed.setCreatedAt(LocalDateTime.now().minusDays(18));
        completed.setUpdatedAt(LocalDateTime.now().minusDays(2));
        completed.setDuration(12);

        when(projectRepository.findById(20L)).thenReturn(Optional.of(target));
        when(candidatureClient.listByFreelancer(77L)).thenReturn(List.of(accepted));
        when(projectRepository.findAllById(any())).thenReturn(List.of(completed));
        when(ratingRepository.averageRatingByFreelancerId(77L)).thenReturn(4.5);

        var batch = service.computeFit(20L, List.of(77L, 77L));

        assertThat(batch.getFreelancers()).hasSize(1);
        assertThat(batch.getFreelancers().get(0).getFreelancerId()).isEqualTo(77L);
        assertThat(batch.getFreelancers().get(0).getSuccessScore()).isBetween(0, 100);
    }

    @Test
    void computeFit_returnsEmptyResult_whenFreelancerIdsEmpty() {
        Project target = newProject(21L);
        when(projectRepository.findById(21L)).thenReturn(Optional.of(target));

        var batch = service.computeFit(21L, List.of());

        assertThat(batch.getProjectId()).isEqualTo(21L);
        assertThat(batch.getFreelancers()).isEmpty();
    }

    @Test
    void submitRating_throwsNotFound_whenProjectMissing() {
        SubmitFreelancerRatingRequest request = new SubmitFreelancerRatingRequest();
        request.setFreelancerId(4L);
        request.setRating(3);
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitRating(404L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void computeFit_returnsLowConfidenceAndNoClientRatingSummary_whenNoPastProjects() {
        Project target = newProject(33L);
        target.setRequiredSkills(List.of("Java", "Angular"));
        target.setDuration(25);

        when(projectRepository.findById(33L)).thenReturn(Optional.of(target));
        when(candidatureClient.listByFreelancer(50L)).thenReturn(List.of());
        when(ratingRepository.averageRatingByFreelancerId(50L)).thenReturn(null);

        var batch = service.computeFit(33L, List.of(50L));

        assertThat(batch.getFreelancers()).hasSize(1);
        var fit = batch.getFreelancers().get(0);
        assertThat(fit.getConfidence()).isEqualTo("LOW");
        assertThat(fit.getPastMissionsConsidered()).isEqualTo(0);
        assertThat(fit.getSummary()).contains("pas encore de notes clients en base");
    }

    private static Project newProject(Long id) {
        Project p = new Project();
        p.setId(id);
        p.setStatus(ProjectStatus.OPEN);
        p.setTitle("Sample project");
        p.setDescription("Build APIs and deliver features quickly.");
        p.setBudget(1200.0);
        p.setDuration(20);
        p.setProjectOwnerId(1L);
        return p;
    }
}
