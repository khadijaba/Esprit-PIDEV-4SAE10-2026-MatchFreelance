package esprit.project.Controllers;

import esprit.project.Service.ProjectSupervisionService;
import esprit.project.dto.CreateDeliverableRequest;
import esprit.project.dto.CreatePhaseMeetingRequest;
import esprit.project.dto.CreatePhaseRequest;
import esprit.project.dto.DecisionCopilotResponse;
import esprit.project.dto.ReviewDeliverableRequest;
import esprit.project.entities.PhaseDeliverable;
import esprit.project.entities.PhaseMeeting;
import esprit.project.entities.ProjectPhase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/phases")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProjectSupervisionController {

    private final ProjectSupervisionService projectSupervisionService;

    @GetMapping
    public List<ProjectPhase> listPhases(@PathVariable Long projectId) {
        return projectSupervisionService.listPhases(projectId);
    }

    @PostMapping
    public ProjectPhase createPhase(@PathVariable Long projectId, @Valid @RequestBody CreatePhaseRequest request) {
        return projectSupervisionService.createPhase(projectId, request);
    }

    @PutMapping("/{phaseId}/start")
    public ProjectPhase startPhase(@PathVariable Long projectId, @PathVariable Long phaseId) {
        return projectSupervisionService.startPhase(projectId, phaseId);
    }

    @PostMapping("/{phaseId}/close")
    public ProjectPhase closePhase(@PathVariable Long projectId, @PathVariable Long phaseId) {
        return projectSupervisionService.closePhase(projectId, phaseId);
    }

    @GetMapping("/{phaseId}/deliverables")
    public List<PhaseDeliverable> listDeliverables(@PathVariable Long projectId, @PathVariable Long phaseId) {
        return projectSupervisionService.listDeliverables(projectId, phaseId);
    }

    @PostMapping("/{phaseId}/deliverables")
    public PhaseDeliverable createDeliverable(
            @PathVariable Long projectId,
            @PathVariable Long phaseId,
            @Valid @RequestBody CreateDeliverableRequest request) {
        return projectSupervisionService.createDeliverable(projectId, phaseId, request);
    }

    @PutMapping("/{phaseId}/deliverables/{deliverableId}/review")
    public PhaseDeliverable reviewDeliverable(
            @PathVariable Long projectId,
            @PathVariable Long phaseId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody ReviewDeliverableRequest request) {
        return projectSupervisionService.reviewDeliverable(projectId, phaseId, deliverableId, request);
    }

    @GetMapping("/{phaseId}/meetings")
    public List<PhaseMeeting> listMeetings(@PathVariable Long projectId, @PathVariable Long phaseId) {
        return projectSupervisionService.listMeetings(projectId, phaseId);
    }

    @GetMapping("/{phaseId}/decision-copilot")
    public DecisionCopilotResponse getDecisionCopilot(@PathVariable Long projectId, @PathVariable Long phaseId) {
        return projectSupervisionService.getDecisionCopilot(projectId, phaseId);
    }

    @PostMapping("/{phaseId}/meetings")
    public PhaseMeeting createMeeting(
            @PathVariable Long projectId,
            @PathVariable Long phaseId,
            @Valid @RequestBody CreatePhaseMeetingRequest request) {
        return projectSupervisionService.createMeeting(projectId, phaseId, request);
    }
}
