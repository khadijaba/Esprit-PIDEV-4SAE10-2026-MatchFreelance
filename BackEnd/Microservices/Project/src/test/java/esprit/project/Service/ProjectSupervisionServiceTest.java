package esprit.project.Service;

import esprit.project.Repositories.PhaseDeliverableRepository;
import esprit.project.Repositories.PhaseMeetingRepository;
import esprit.project.Repositories.ProjectPhaseRepository;
import esprit.project.Repositories.ProjectRepository;
import esprit.project.dto.CreatePhaseRequest;
import esprit.project.dto.DecisionCopilotResponse;
import esprit.project.entities.DeliverableReviewStatus;
import esprit.project.entities.PhaseDeliverable;
import esprit.project.entities.PhaseMeeting;
import esprit.project.entities.Project;
import esprit.project.entities.ProjectPhase;
import esprit.project.entities.ProjectPhaseStatus;
import esprit.project.entities.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectSupervisionServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectPhaseRepository projectPhaseRepository;
    @Mock
    private PhaseDeliverableRepository phaseDeliverableRepository;
    @Mock
    private PhaseMeetingRepository phaseMeetingRepository;

    @InjectMocks
    private ProjectSupervisionService service;

    @Test
    void createPhase_savesTrimmedName_whenProjectInProgress() {
        Project project = newProject(1L, ProjectStatus.IN_PROGRESS);

        CreatePhaseRequest request = new CreatePhaseRequest();
        request.setName("  Discovery  ");
        request.setDescription("Initial scope");
        request.setPhaseOrder(1);
        request.setStartDate(LocalDateTime.now());
        request.setDueDate(LocalDateTime.now().plusDays(7));

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectPhaseRepository.findByProjectAndPhaseOrder(project, 1)).thenReturn(Optional.empty());
        when(projectPhaseRepository.save(any(ProjectPhase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectPhase saved = service.createPhase(1L, request);

        assertThat(saved.getName()).isEqualTo("Discovery");
        assertThat(saved.getStatus()).isEqualTo(ProjectPhaseStatus.PLANNED);
        assertThat(saved.getProject()).isSameAs(project);
        verify(projectPhaseRepository).save(any(ProjectPhase.class));
    }

    @Test
    void startPhase_throwsWhenPreviousPhaseNotApproved() {
        Project project = newProject(10L, ProjectStatus.IN_PROGRESS);
        ProjectPhase current = newPhase(20L, project, 2, ProjectPhaseStatus.PLANNED);
        ProjectPhase previous = newPhase(21L, project, 1, ProjectPhaseStatus.IN_PROGRESS);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectPhaseRepository.findById(20L)).thenReturn(Optional.of(current));
        when(projectPhaseRepository.findByProjectAndPhaseOrder(project, 1)).thenReturn(Optional.of(previous));

        assertThatThrownBy(() -> service.startPhase(10L, 20L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Previous phase must be APPROVED");

        verify(projectPhaseRepository, never()).save(any(ProjectPhase.class));
    }

    @Test
    void closePhase_setsApprovedStatus_whenDeliverablesAcceptedAndMeetingExists() {
        Project project = newProject(5L, ProjectStatus.IN_PROGRESS);
        ProjectPhase phase = newPhase(6L, project, 1, ProjectPhaseStatus.IN_PROGRESS);

        PhaseDeliverable deliverable = new PhaseDeliverable();
        deliverable.setId(100L);
        deliverable.setPhase(phase);
        deliverable.setReviewStatus(DeliverableReviewStatus.ACCEPTED);

        PhaseMeeting meeting = new PhaseMeeting();
        meeting.setId(200L);
        meeting.setPhase(phase);

        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(projectPhaseRepository.findById(6L)).thenReturn(Optional.of(phase));
        when(phaseDeliverableRepository.findByPhase(phase)).thenReturn(List.of(deliverable));
        when(phaseMeetingRepository.findByPhase(phase)).thenReturn(List.of(meeting));
        when(projectPhaseRepository.save(phase)).thenReturn(phase);

        ProjectPhase out = service.closePhase(5L, 6L);

        assertThat(out.getStatus()).isEqualTo(ProjectPhaseStatus.APPROVED);
        assertThat(out.getApprovedAt()).isNotNull();
    }

    @Test
    void getDecisionCopilot_returnsAcceptPhaseRecommendation_whenAllSignalsAreGreen() {
        Project project = newProject(8L, ProjectStatus.IN_PROGRESS);
        ProjectPhase phase = newPhase(9L, project, 3, ProjectPhaseStatus.IN_PROGRESS);
        phase.setDueDate(LocalDateTime.now().plusDays(2));

        PhaseDeliverable d1 = new PhaseDeliverable();
        d1.setPhase(phase);
        d1.setReviewStatus(DeliverableReviewStatus.ACCEPTED);
        PhaseDeliverable d2 = new PhaseDeliverable();
        d2.setPhase(phase);
        d2.setReviewStatus(DeliverableReviewStatus.ACCEPTED);

        PhaseMeeting meeting = new PhaseMeeting();
        meeting.setPhase(phase);

        when(projectRepository.findById(8L)).thenReturn(Optional.of(project));
        when(projectPhaseRepository.findById(9L)).thenReturn(Optional.of(phase));
        when(phaseDeliverableRepository.findByPhase(phase)).thenReturn(List.of(d1, d2));
        when(phaseMeetingRepository.findByPhase(phase)).thenReturn(List.of(meeting));

        DecisionCopilotResponse response = service.getDecisionCopilot(8L, 9L);

        assertThat(response.getRecommendation()).isEqualTo("ACCEPT_PHASE");
        assertThat(response.getSuggestedActions()).contains("Clôturer la phase.");
        assertThat(response.getOwnerMessageDraft()).contains("validée");
    }

    @Test
    void getDecisionCopilot_returnsSplitPhase_whenManyDeliverablesAndLowAcceptance() {
        Project project = newProject(18L, ProjectStatus.IN_PROGRESS);
        ProjectPhase phase = newPhase(19L, project, 2, ProjectPhaseStatus.IN_PROGRESS);
        phase.setDueDate(LocalDateTime.now().plusDays(1));

        PhaseDeliverable acceptedOne = new PhaseDeliverable();
        acceptedOne.setPhase(phase);
        acceptedOne.setReviewStatus(DeliverableReviewStatus.ACCEPTED);

        PhaseDeliverable pendingOne = new PhaseDeliverable();
        pendingOne.setPhase(phase);
        pendingOne.setReviewStatus(DeliverableReviewStatus.PENDING);

        List<PhaseDeliverable> deliverables = List.of(
                acceptedOne, pendingOne, pendingOne, pendingOne, pendingOne, pendingOne
        );

        when(projectRepository.findById(18L)).thenReturn(Optional.of(project));
        when(projectPhaseRepository.findById(19L)).thenReturn(Optional.of(phase));
        when(phaseDeliverableRepository.findByPhase(phase)).thenReturn(deliverables);
        when(phaseMeetingRepository.findByPhase(phase)).thenReturn(List.of(new PhaseMeeting()));

        DecisionCopilotResponse response = service.getDecisionCopilot(18L, 19L);

        assertThat(response.getRecommendation()).isEqualTo("SPLIT_PHASE");
        assertThat(response.getSummary()).contains("découpage recommandé");
    }

    @Test
    void getDecisionCopilot_returnsEscalateRisk_whenPhaseIsOverdue() {
        Project project = newProject(30L, ProjectStatus.IN_PROGRESS);
        ProjectPhase phase = newPhase(31L, project, 1, ProjectPhaseStatus.IN_PROGRESS);
        phase.setDueDate(LocalDateTime.now().minusDays(1));

        PhaseDeliverable pending = new PhaseDeliverable();
        pending.setPhase(phase);
        pending.setReviewStatus(DeliverableReviewStatus.PENDING);

        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));
        when(projectPhaseRepository.findById(31L)).thenReturn(Optional.of(phase));
        when(phaseDeliverableRepository.findByPhase(phase)).thenReturn(List.of(pending));
        when(phaseMeetingRepository.findByPhase(phase)).thenReturn(List.of());

        DecisionCopilotResponse response = service.getDecisionCopilot(30L, 31L);

        assertThat(response.getRecommendation()).isEqualTo("ESCALATE_RISK");
        assertThat(response.getSuggestedActions()).anyMatch(a -> a.contains("24h"));
    }

    private static Project newProject(Long id, ProjectStatus status) {
        Project project = new Project();
        project.setId(id);
        project.setStatus(status);
        return project;
    }

    private static ProjectPhase newPhase(Long id, Project project, int order, ProjectPhaseStatus status) {
        ProjectPhase phase = new ProjectPhase();
        phase.setId(id);
        phase.setProject(project);
        phase.setPhaseOrder(order);
        phase.setStatus(status);
        phase.setName("Phase " + order);
        return phase;
    }
}
