package esprit.project.Service;

import esprit.project.Repositories.ClientFreelancerRatingRepository;
import esprit.project.Repositories.ProjectRepository;
import esprit.project.client.CandidatureClient;
import esprit.project.dto.FreelancerFitBatchDto;
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
    void computeFit_returnsEmptyRows_whenFreelancerIdsIsNull() {
        Project target = newProject(1L, ProjectStatus.IN_PROGRESS, List.of("Java"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(target));

        FreelancerFitBatchDto out = service.computeFit(1L, null);

        assertThat(out.getProjectId()).isEqualTo(1L);
        assertThat(out.getFreelancers()).isEmpty();
    }

    @Test
    void computeFit_buildsSingleRow_forDistinctFreelancersAndAcceptedHistory() {
        Project target = newProject(1L, ProjectStatus.IN_PROGRESS, List.of("Java", "Spring"));
        Project past = newProject(2L, ProjectStatus.COMPLETED, List.of("Java"));
        past.setCreatedAt(LocalDateTime.now().minusDays(12));
        past.setUpdatedAt(LocalDateTime.now().minusDays(2));
        past.setDuration(8);

        CandidatureSummaryDto accepted = new CandidatureSummaryDto();
        accepted.setProjectId(2L);
        accepted.setFreelancerId(99L);
        accepted.setStatus("ACCEPTED");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(target));
        when(candidatureClient.listByFreelancer(99L)).thenReturn(List.of(accepted));
        when(projectRepository.findAllById(any())).thenReturn(List.of(past));
        when(ratingRepository.averageRatingByFreelancerId(99L)).thenReturn(4.5);

        FreelancerFitBatchDto out = service.computeFit(1L, List.of(99L, 99L));

        assertThat(out.getFreelancers()).hasSize(1);
        assertThat(out.getFreelancers().get(0).getFreelancerId()).isEqualTo(99L);
        assertThat(out.getFreelancers().get(0).getPastMissionsConsidered()).isEqualTo(1);
        assertThat(out.getFreelancers().get(0).getSummary()).contains("note moyenne clients");
    }

    @Test
    void submitRating_throwsBadRequest_whenFieldsMissing() {
        when(projectRepository.findById(7L)).thenReturn(Optional.of(newProject(7L, ProjectStatus.OPEN, List.of())));

        SubmitFreelancerRatingRequest request = new SubmitFreelancerRatingRequest();
        request.setFreelancerId(null);
        request.setRating(5);

        assertThatThrownBy(() -> service.submitRating(7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("freelancerId and rating required");
    }

    @Test
    void submitRating_updatesExistingRow_whenAlreadyRated() {
        when(projectRepository.findById(8L)).thenReturn(Optional.of(newProject(8L, ProjectStatus.OPEN, List.of())));

        ClientFreelancerRating existing = new ClientFreelancerRating();
        existing.setProjectId(8L);
        existing.setFreelancerId(55L);
        existing.setRating(3);

        when(ratingRepository.findByProjectIdAndFreelancerId(8L, 55L)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(existing)).thenReturn(existing);

        SubmitFreelancerRatingRequest request = new SubmitFreelancerRatingRequest();
        request.setFreelancerId(55L);
        request.setRating(5);

        ClientFreelancerRating out = service.submitRating(8L, request);

        assertThat(out.getRating()).isEqualTo(5);
        verify(ratingRepository).save(existing);
    }

    private static Project newProject(Long id, ProjectStatus status, List<String> skills) {
        Project p = new Project();
        p.setId(id);
        p.setTitle("Project " + id);
        p.setDescription("Detailed description for project " + id);
        p.setBudget(6000.0);
        p.setDuration(20);
        p.setStatus(status);
        p.setProjectOwnerId(10L);
        p.setRequiredSkills(skills);
        return p;
    }
}
