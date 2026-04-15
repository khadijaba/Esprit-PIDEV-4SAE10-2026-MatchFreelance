package esprit.project.Service;

import esprit.project.Repositories.PhaseDeliverableRepository;
import esprit.project.Repositories.PhaseMeetingRepository;
import esprit.project.Repositories.ProjectPhaseRepository;
import esprit.project.Repositories.ProjectRepository;
import esprit.project.dto.CreateDeliverableRequest;
import esprit.project.dto.CreatePhaseMeetingRequest;
import esprit.project.dto.CreatePhaseRequest;
import esprit.project.dto.DecisionCopilotResponse;
import esprit.project.dto.ReviewDeliverableRequest;
import esprit.project.entities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectSupervisionService {

    private final ProjectRepository projectRepository;
    private final ProjectPhaseRepository projectPhaseRepository;
    private final PhaseDeliverableRepository phaseDeliverableRepository;
    private final PhaseMeetingRepository phaseMeetingRepository;

    @Transactional(readOnly = true)
    public List<ProjectPhase> listPhases(Long projectId) {
        Project project = loadProject(projectId);
        return projectPhaseRepository.findByProjectOrderByPhaseOrderAsc(project);
    }

    @Transactional
    public ProjectPhase createPhase(Long projectId, CreatePhaseRequest request) {
        Project project = loadProject(projectId);
        ensureProjectInProgress(project);
        projectPhaseRepository.findByProjectAndPhaseOrder(project, request.getPhaseOrder())
                .ifPresent(phase -> {
                    throw new RuntimeException("Phase order already exists for this project");
                });

        ProjectPhase phase = new ProjectPhase();
        phase.setProject(project);
        phase.setName(request.getName().trim());
        phase.setDescription(request.getDescription());
        phase.setPhaseOrder(request.getPhaseOrder());
        phase.setStartDate(request.getStartDate());
        phase.setDueDate(request.getDueDate());
        phase.setStatus(ProjectPhaseStatus.PLANNED);
        return projectPhaseRepository.save(phase);
    }

    @Transactional
    public ProjectPhase startPhase(Long projectId, Long phaseId) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        if (phase.getStatus() != ProjectPhaseStatus.PLANNED && phase.getStatus() != ProjectPhaseStatus.BLOCKED) {
            throw new RuntimeException("Only PLANNED/BLOCKED phase can be started");
        }

        if (phase.getPhaseOrder() > 1) {
            ProjectPhase previous = projectPhaseRepository
                    .findByProjectAndPhaseOrder(phase.getProject(), phase.getPhaseOrder() - 1)
                    .orElseThrow(() -> new RuntimeException("Previous phase is missing"));
            if (previous.getStatus() != ProjectPhaseStatus.APPROVED) {
                throw new RuntimeException("Previous phase must be APPROVED before starting this phase");
            }
        }

        phase.setStatus(ProjectPhaseStatus.IN_PROGRESS);
        return projectPhaseRepository.save(phase);
    }

    @Transactional(readOnly = true)
    public List<PhaseDeliverable> listDeliverables(Long projectId, Long phaseId) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        return phaseDeliverableRepository.findByPhase(phase);
    }

    @Transactional
    public PhaseDeliverable createDeliverable(Long projectId, Long phaseId, CreateDeliverableRequest request) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        if (phase.getStatus() == ProjectPhaseStatus.APPROVED) {
            throw new RuntimeException("Cannot add deliverable to approved phase");
        }
        PhaseDeliverable deliverable = new PhaseDeliverable();
        deliverable.setPhase(phase);
        deliverable.setTitle(request.getTitle().trim());
        deliverable.setDescription(request.getDescription());
        deliverable.setType(request.getType());
        deliverable.setReviewStatus(DeliverableReviewStatus.PENDING);
        deliverable.setSubmittedAt(LocalDateTime.now());
        return phaseDeliverableRepository.save(deliverable);
    }

    @Transactional
    public PhaseDeliverable reviewDeliverable(Long projectId, Long phaseId, Long deliverableId, ReviewDeliverableRequest request) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        PhaseDeliverable deliverable = phaseDeliverableRepository.findById(deliverableId)
                .orElseThrow(() -> new RuntimeException("Deliverable not found"));
        if (!deliverable.getPhase().getId().equals(phase.getId())) {
            throw new RuntimeException("Deliverable does not belong to this phase");
        }
        deliverable.setReviewStatus(request.getReviewStatus());
        deliverable.setReviewComment(request.getReviewComment());
        deliverable.setReviewedAt(LocalDateTime.now());

        if (request.getReviewStatus() == DeliverableReviewStatus.CHANGES_REQUESTED) {
            phase.setStatus(ProjectPhaseStatus.BLOCKED);
            projectPhaseRepository.save(phase);
        } else if (phase.getStatus() == ProjectPhaseStatus.BLOCKED) {
            phase.setStatus(ProjectPhaseStatus.IN_PROGRESS);
            projectPhaseRepository.save(phase);
        }

        return phaseDeliverableRepository.save(deliverable);
    }

    @Transactional(readOnly = true)
    public List<PhaseMeeting> listMeetings(Long projectId, Long phaseId) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        return phaseMeetingRepository.findByPhase(phase);
    }

    @Transactional
    public PhaseMeeting createMeeting(Long projectId, Long phaseId, CreatePhaseMeetingRequest request) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        if (phase.getStatus() == ProjectPhaseStatus.PLANNED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start phase before scheduling meeting");
        }
        PhaseMeeting meeting = new PhaseMeeting();
        meeting.setPhase(phase);
        meeting.setMeetingAt(request.getMeetingAt());
        meeting.setAgenda(request.getAgenda());
        meeting.setSummary(request.getSummary());
        meeting.setDecision(request.getDecision());
        return phaseMeetingRepository.save(meeting);
    }

    @Transactional
    public ProjectPhase closePhase(Long projectId, Long phaseId) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        List<PhaseDeliverable> deliverables = phaseDeliverableRepository.findByPhase(phase);
        if (deliverables.isEmpty()) {
            throw new RuntimeException("At least one deliverable is required before closing the phase");
        }
        boolean allAccepted = deliverables.stream()
                .allMatch(d -> d.getReviewStatus() == DeliverableReviewStatus.ACCEPTED);
        if (!allAccepted) {
            throw new RuntimeException("All deliverables must be ACCEPTED before closing the phase");
        }
        List<PhaseMeeting> meetings = phaseMeetingRepository.findByPhase(phase);
        if (meetings.isEmpty()) {
            throw new RuntimeException("At least one owner-freelancer meeting is required before closing the phase");
        }

        phase.setStatus(ProjectPhaseStatus.APPROVED);
        phase.setApprovedAt(LocalDateTime.now());
        return projectPhaseRepository.save(phase);
    }

    @Transactional
    public void deletePhase(Long projectId, Long phaseId) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        ensureProjectInProgress(phase.getProject());

        if (phase.getStatus() != ProjectPhaseStatus.PLANNED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PLANNED phases can be deleted");
        }

        projectPhaseRepository.delete(phase);
    }

    @Transactional(readOnly = true)
    public DecisionCopilotResponse getDecisionCopilot(Long projectId, Long phaseId) {
        ProjectPhase phase = loadProjectPhase(projectId, phaseId);
        List<PhaseDeliverable> deliverables = phaseDeliverableRepository.findByPhase(phase);
        List<PhaseMeeting> meetings = phaseMeetingRepository.findByPhase(phase);

        int total = deliverables.size();
        long accepted = deliverables.stream().filter(d -> d.getReviewStatus() == DeliverableReviewStatus.ACCEPTED).count();
        long changes = deliverables.stream().filter(d -> d.getReviewStatus() == DeliverableReviewStatus.CHANGES_REQUESTED).count();
        long pending = deliverables.stream().filter(d -> d.getReviewStatus() == DeliverableReviewStatus.PENDING).count();
        boolean overdue = phase.getDueDate() != null && phase.getDueDate().isBefore(LocalDateTime.now())
                && phase.getStatus() != ProjectPhaseStatus.APPROVED;

        List<String> reasons = new ArrayList<>();
        List<String> actions = new ArrayList<>();
        String recommendation;
        int confidence;
        String summary;

        if (phase.getStatus() == ProjectPhaseStatus.APPROVED) {
            recommendation = "NO_ACTION";
            confidence = 95;
            summary = "Phase déjà approuvée, aucune action critique nécessaire.";
            reasons.add("Le statut actuel est APPROVED.");
            actions.add("Passer à la phase suivante.");
            return buildCopilot(recommendation, confidence, summary, reasons, actions);
        }

        if (overdue) {
            recommendation = "ESCALATE_RISK";
            confidence = 90;
            summary = "Risque de retard élevé sur cette phase.";
            reasons.add("La date d'échéance de la phase est dépassée.");
            if (pending > 0) reasons.add("Des livrables sont encore en attente de review.");
            if (changes > 0) reasons.add("Des livrables demandent des corrections.");
            actions.add("Planifier un meeting d'alignement sous 24h.");
            actions.add("Réduire le scope de la phase ou décaler officiellement l'échéance.");
            return buildCopilot(recommendation, confidence, summary, reasons, actions);
        }

        if (total == 0) {
            recommendation = "REQUEST_CHANGES";
            confidence = 88;
            summary = "La phase ne peut pas être validée sans livrables.";
            reasons.add("Aucun livrable soumis sur cette phase.");
            actions.add("Demander au freelancer de soumettre au moins un livrable concret.");
            actions.add("Préciser les critères d'acceptance attendus.");
            return buildCopilot(recommendation, confidence, summary, reasons, actions);
        }

        if (changes > 0) {
            recommendation = "REQUEST_CHANGES";
            confidence = 86;
            summary = "Des corrections sont nécessaires avant validation.";
            reasons.add(changes + " livrable(s) ont le statut CHANGES_REQUESTED.");
            actions.add("Envoyer une synthèse de corrections priorisées.");
            actions.add("Planifier un point rapide de suivi pour débloquer la phase.");
            return buildCopilot(recommendation, confidence, summary, reasons, actions);
        }

        boolean allAccepted = accepted == total;
        if (allAccepted && meetings.isEmpty()) {
            recommendation = "REQUEST_CHANGES";
            confidence = 80;
            summary = "Les livrables sont validés, mais le meeting de revue est manquant.";
            reasons.add("Aucun meeting owner-freelancer enregistré sur cette phase.");
            actions.add("Planifier un meeting de clôture avant approbation.");
            return buildCopilot(recommendation, confidence, summary, reasons, actions);
        }

        if (allAccepted && !meetings.isEmpty()) {
            recommendation = "ACCEPT_PHASE";
            confidence = 92;
            summary = "La phase semble prête à être approuvée.";
            reasons.add("Tous les livrables sont ACCEPTED.");
            reasons.add("Au moins un meeting de revue est enregistré.");
            actions.add("Clôturer la phase.");
            actions.add("Démarrer la phase suivante.");
            return buildCopilot(recommendation, confidence, summary, reasons, actions);
        }

        double acceptanceRatio = total == 0 ? 0.0 : (double) accepted / total;
        if (total >= 6 && acceptanceRatio < 0.4) {
            recommendation = "SPLIT_PHASE";
            confidence = 75;
            summary = "La phase est trop lourde: découpage recommandé.";
            reasons.add("Beaucoup de livrables (" + total + ") avec faible taux d'acceptation.");
            reasons.add("Risque de blocage si tout reste dans la même phase.");
            actions.add("Scinder la phase en sous-phases plus courtes.");
            actions.add("Prioriser un sous-ensemble de livrables critiques.");
            return buildCopilot(recommendation, confidence, summary, reasons, actions);
        }

        recommendation = "REQUEST_CHANGES";
        confidence = 70;
        summary = "La phase est en progression, mais pas encore prête pour approbation.";
        if (pending > 0) reasons.add(pending + " livrable(s) encore en attente de review.");
        if (accepted > 0) reasons.add(accepted + " livrable(s) déjà validé(s).");
        actions.add("Finaliser la review des livrables restants.");
        actions.add("Confirmer les points ouverts en meeting.");
        return buildCopilot(recommendation, confidence, summary, reasons, actions);
    }

    private static DecisionCopilotResponse buildCopilot(
            String recommendation,
            Integer confidence,
            String summary,
            List<String> reasons,
            List<String> actions) {
        String messageDraft = buildOwnerMessageDraft(recommendation, reasons, actions);
        return DecisionCopilotResponse.builder()
                .recommendation(recommendation)
                .confidence(confidence)
                .summary(summary)
                .reasons(reasons)
                .suggestedActions(actions)
                .ownerMessageDraft(messageDraft)
                .build();
    }

    private static String buildOwnerMessageDraft(String recommendation, List<String> reasons, List<String> actions) {
        String tone = switch (recommendation) {
            case "ACCEPT_PHASE" -> "La phase est validée. Merci pour la qualité du travail.";
            case "ESCALATE_RISK" -> "Nous avons un risque de retard à traiter immédiatement.";
            case "SPLIT_PHASE" -> "Je propose de découper cette phase pour mieux sécuriser la livraison.";
            default -> "Merci de prendre en compte les ajustements ci-dessous avant validation.";
        };
        String topReason = reasons.isEmpty() ? "" : " Constat: " + reasons.get(0) + ".";
        String topAction = actions.isEmpty() ? "" : " Action prioritaire: " + actions.get(0) + ".";
        return tone + topReason + topAction;
    }

    private Project loadProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    private ProjectPhase loadProjectPhase(Long projectId, Long phaseId) {
        Project project = loadProject(projectId);
        ProjectPhase phase = projectPhaseRepository.findById(phaseId)
                .orElseThrow(() -> new RuntimeException("Phase not found"));
        if (!phase.getProject().getId().equals(project.getId())) {
            throw new RuntimeException("Phase does not belong to this project");
        }
        return phase;
    }

    private static void ensureProjectInProgress(Project project) {
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            throw new RuntimeException("Project supervision is available only when project is IN_PROGRESS");
        }
    }
}
