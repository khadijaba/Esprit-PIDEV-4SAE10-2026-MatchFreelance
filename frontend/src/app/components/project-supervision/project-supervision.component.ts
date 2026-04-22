import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ProjectService } from '../../services/project.service';
import { ProjectSupervisionService } from '../../services/project-supervision.service';
import { CandidatureService } from '../../services/candidature.service';
import { NotificationService } from '../../services/notification.service';
import { ToastService } from '../../services/toast.service';
import { TeamAiService } from '../../services/team-ai.service';
import { httpErrorMessage } from '../../utils/http-error.util';
import { Project } from '../../models/project.model';
import { PlanningAssistantResponse, PlanningPhaseProposal, ScheduleOverrunResponse } from '../../models/team-ai.model';
import {
  CreateDeliverableRequest,
  CreatePhaseMeetingRequest,
  CreatePhaseRequest,
  DecisionCopilotRecommendation,
  DecisionCopilotResponse,
  DeliverableReviewStatus,
  DeliverableType,
  PhaseDeliverable,
  PhaseMeeting,
  PhaseMeetingDecision,
  ProjectPhase,
  ProjectPhaseStatus,
  ReviewDeliverableRequest,
} from '../../models/project-supervision.model';

@Component({
  selector: 'app-project-supervision',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-supervision.component.html',
})
export class ProjectSupervisionComponent implements OnInit {
  projectId!: number;
  private clientId!: number;
  project?: Project;
  loading = true;
  phases: ProjectPhase[] = [];
  selectedPhaseId: number | null = null;
  deliverables: PhaseDeliverable[] = [];
  meetings: PhaseMeeting[] = [];

  creatingPhase = false;
  creatingDeliverable = false;
  creatingMeeting = false;
  closingPhase = false;
  deletingPhaseId: number | null = null;
  startingPhaseId: number | null = null;
  reviewingDeliverableId: number | null = null;
  copilotLoading = false;
  decisionCopilot: DecisionCopilotResponse | null = null;

  scheduleRiskLoading = false;
  scheduleAssessment: ScheduleOverrunResponse | null = null;
  scheduleRiskError: string | null = null;
  planningLoading = false;
  planningApplying = false;
  planningProposal: PlanningAssistantResponse | null = null;
  planningError: string | null = null;
  planningUseLlm = true;

  newPhase: CreatePhaseRequest = {
    name: '',
    description: '',
    phaseOrder: 1,
    startDate: '',
    dueDate: '',
  };

  newDeliverable: CreateDeliverableRequest = {
    title: '',
    description: '',
    type: 'DOC',
  };

  newMeeting: CreatePhaseMeetingRequest = {
    meetingAt: '',
    agenda: '',
    summary: '',
    decision: 'GO',
  };

  readonly deliverableTypes: DeliverableType[] = ['DOC', 'DESIGN', 'CODE', 'DEMO', 'REPORT'];
  readonly reviewStatuses: DeliverableReviewStatus[] = ['PENDING', 'CHANGES_REQUESTED', 'ACCEPTED'];
  readonly meetingDecisions: PhaseMeetingDecision[] = ['GO', 'GO_WITH_CHANGES', 'NO_GO'];

  constructor(
    private route: ActivatedRoute,
    private auth: AuthService,
    private projectService: ProjectService,
    private supervisionService: ProjectSupervisionService,
    private candidatureService: CandidatureService,
    private notificationService: NotificationService,
    private toast: ToastService,
    private teamAi: TeamAiService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    if (!user?.id) {
      this.toast.error('Connectez-vous pour superviser ce projet.');
      return;
    }
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.projectId) {
      this.toast.error('Projet invalide.');
      return;
    }
    this.clientId = Number(user.id);
    void this.auth.ensureFreshTokenIfNeeded();
    this.loadAll();
  }

  get selectedPhase(): ProjectPhase | undefined {
    return this.phases.find((p) => p.id === this.selectedPhaseId);
  }

  loadAll(): void {
    this.loading = true;
    this.projectService.getById(this.projectId).subscribe({
      next: (project) => {
        this.project = project;
        this.loadPhases();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.toast.error(httpErrorMessage(err, 'Impossible de charger le projet.'));
      },
    });
  }

  loadPhases(): void {
    this.supervisionService.listPhases(this.projectId).subscribe({
      next: (phases) => {
        this.phases = [...phases].sort((a, b) => a.phaseOrder - b.phaseOrder);
        if (this.phases.length && !this.selectedPhaseId) {
          this.selectPhase(this.phases[0].id);
        } else if (this.selectedPhaseId) {
          this.selectPhase(this.selectedPhaseId);
        } else {
          this.deliverables = [];
          this.meetings = [];
          this.loading = false;
          this.loadScheduleRisk();
        }
        this.newPhase.phaseOrder = this.nextPhaseOrder();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.toast.error(httpErrorMessage(err, 'Impossible de charger les phases.'));
      },
    });
  }

  selectPhase(phaseId: number): void {
    this.selectedPhaseId = phaseId;
    this.loading = true;
    forkJoin({
      deliverables: this.supervisionService.listDeliverables(this.projectId, phaseId),
      meetings: this.supervisionService.listMeetings(this.projectId, phaseId),
    }).subscribe({
      next: ({ deliverables, meetings }) => {
        this.deliverables = deliverables;
        this.meetings = meetings;
        this.loading = false;
        this.loadScheduleRisk();
        this.loadDecisionCopilot();
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.toast.error(httpErrorMessage(err, 'Impossible de charger les détails de phase.'));
      },
    });
  }

  /** Indicateur temps vs avancement (service Python Team AI, données à jour via la gateway). */
  loadScheduleRisk(): void {
    this.scheduleRiskLoading = true;
    this.scheduleRiskError = null;
    this.teamAi.scheduleOverrunAssessment({ projectId: this.projectId }).subscribe({
      next: (data) => {
        this.scheduleAssessment = data;
        this.scheduleRiskLoading = false;
      },
      error: () => {
        this.scheduleAssessment = null;
        this.scheduleRiskLoading = false;
        this.scheduleRiskError =
          'Indicateur de planning indisponible. Vérifiez que l’API Gateway (8086) et le service Team AI (Python, port 5000) sont démarrés, puis réessayez.';
      },
    });
  }

  generateInitialPlanningWithAi(): void {
    if (!this.project) return;
    this.planningLoading = true;
    this.planningError = null;
    this.planningProposal = null;
    this.teamAi
      .planningInitialPlan({
        projectTitle: this.project.title,
        projectDescription: this.project.description,
        durationDays: this.project.duration,
        requiredSkills: this.project.requiredSkills ?? [],
        useLlm: this.planningUseLlm,
      })
      .subscribe({
        next: (res) => {
          this.planningLoading = false;
          this.planningProposal = res;
          this.toast.success(`Plan initial généré (${res.llmUsed ? res.llmBackend ?? 'LLM' : 'heuristique'}).`);
        },
        error: (err: HttpErrorResponse) => {
          this.planningLoading = false;
          this.planningError = httpErrorMessage(
            err,
            'Assistant planning indisponible (génération initiale). Vérifiez Team AI et /api/ai/planning-assistant/initial-plan.'
          );
        },
      });
  }

  generateAdjustPlanningWithAi(): void {
    if (!this.phases.length) {
      this.toast.error('Aucune phase à réajuster. Générez d’abord un plan initial.');
      return;
    }
    this.planningLoading = true;
    this.planningError = null;
    this.planningProposal = null;
    const currentPhases = this.phases.map((p) => ({
      phaseOrder: p.phaseOrder,
      name: p.name,
      plannedDays: this.estimatePhaseDays(p.startDate, p.dueDate),
      startDate: p.startDate,
      dueDate: p.dueDate,
      milestones: [],
      deliverables: [],
      acceptanceCriteria: [],
    }));
    this.teamAi
      .planningAdjustPlan({
        currentPhases,
        scheduleAssessment: this.scheduleAssessment as unknown as Record<string, unknown>,
        useLlm: this.planningUseLlm,
      })
      .subscribe({
        next: (res) => {
          this.planningLoading = false;
          this.planningProposal = res;
          this.toast.success(`Réajustement proposé (${res.llmUsed ? res.llmBackend ?? 'LLM' : 'heuristique'}).`);
        },
        error: (err: HttpErrorResponse) => {
          this.planningLoading = false;
          this.planningError = httpErrorMessage(
            err,
            'Assistant planning indisponible (réajustement). Vérifiez Team AI et /api/ai/planning-assistant/adjust-plan.'
          );
        },
      });
  }

  async applyPlanningProposal(): Promise<void> {
    if (!this.planningProposal?.phases?.length) {
      this.toast.error('Aucune proposition à appliquer.');
      return;
    }
    this.planningApplying = true;
    try {
      const plannedToReplace = this.phases.filter((p) => p.status === 'PLANNED');
      for (const p of plannedToReplace) {
        await firstValueFrom(this.supervisionService.deletePhase(this.projectId, p.id));
      }
      if (plannedToReplace.length > 0) {
        this.toast.show(`${plannedToReplace.length} phase(s) PLANNED supprimée(s) avant application IA.`, 'info');
      }

      const baseOrder = this.nextPhaseOrder() - 1;
      for (let i = 0; i < this.planningProposal.phases.length; i++) {
        const ph = this.planningProposal.phases[i];
        const payload = this.toCreatePhasePayload(ph, baseOrder + i + 1);
        await firstValueFrom(this.supervisionService.createPhase(this.projectId, payload));
      }
      this.toast.success(`${this.planningProposal.phases.length} phase(s) IA appliquée(s).`);
      this.planningProposal = null;
      this.loadPhases();
    } catch (err) {
      this.toast.error(httpErrorMessage(err, 'Application du planning IA impossible.'));
    } finally {
      this.planningApplying = false;
    }
  }

  clearPlanningProposal(): void {
    this.planningProposal = null;
    this.planningError = null;
  }

  private toCreatePhasePayload(p: PlanningPhaseProposal, forcedOrder: number): CreatePhaseRequest {
    const liv = p.deliverables?.map((d) => `${d.title} [${d.type}]`).join(' ; ') || 'N/A';
    const crit = p.acceptanceCriteria?.join(' ; ') || 'N/A';
    return {
      name: (p.name || `Phase ${forcedOrder}`).trim(),
      phaseOrder: forcedOrder,
      description: `IA planning\n- Milestones: ${(p.milestones || []).join(' | ') || 'N/A'}\n- Livrables: ${liv}\n- Critères: ${crit}`,
      startDate: this.toBackendLocalDateTime(p.startDate),
      dueDate: this.toBackendLocalDateTime(p.dueDate),
    };
  }

  /**
   * Backend Project attend LocalDateTime (sans timezone) pour CreatePhaseRequest.
   * Ex: "2026-04-15T10:30:00"
   */
  private toBackendLocalDateTime(v?: string | null): string | undefined {
    if (!v) return undefined;
    const d = new Date(v);
    if (!Number.isFinite(d.getTime())) {
      const s = String(v).trim();
      // Si déjà proche du format local, garder tel quel.
      if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(s)) return s.length === 16 ? `${s}:00` : s;
      return undefined;
    }
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const hh = String(d.getHours()).padStart(2, '0');
    const mi = String(d.getMinutes()).padStart(2, '0');
    const ss = String(d.getSeconds()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}`;
  }

  private estimatePhaseDays(startDate?: string | null, dueDate?: string | null): number {
    if (!startDate || !dueDate) return 5;
    const s = new Date(startDate).getTime();
    const d = new Date(dueDate).getTime();
    if (!Number.isFinite(s) || !Number.isFinite(d) || d <= s) return 5;
    return Math.max(1, Math.round((d - s) / 86400000));
  }

  scheduleRiskCardClass(): string {
    const a = this.scheduleAssessment;
    if (!a) return 'border-gray-200 bg-white';
    switch (a.scheduleRiskLevel) {
      case 'AT_RISK':
        return 'border-red-200 bg-red-50';
      case 'WATCH':
        return 'border-amber-200 bg-amber-50';
      default:
        return 'border-emerald-100 bg-emerald-50/60';
    }
  }

  scheduleRiskTitle(): string {
    return this.scheduleAssessment?.banner?.title ?? 'Planning & avancement';
  }

  loadDecisionCopilot(): void {
    if (!this.selectedPhaseId) {
      this.decisionCopilot = null;
      return;
    }
    this.copilotLoading = true;
    this.supervisionService.getDecisionCopilot(this.projectId, this.selectedPhaseId).subscribe({
      next: (data) => {
        this.copilotLoading = false;
        this.decisionCopilot = data;
      },
      error: (err: HttpErrorResponse) => {
        this.copilotLoading = false;
        this.decisionCopilot = null;
        this.toast.error(httpErrorMessage(err, 'Decision Copilot indisponible.'));
      },
    });
  }

  createPhase(): void {
    if (!this.newPhase.name.trim()) {
      this.toast.error('Nom de phase requis.');
      return;
    }
    this.creatingPhase = true;
    const payload: CreatePhaseRequest = {
      ...this.newPhase,
      name: this.newPhase.name.trim(),
      description: this.newPhase.description?.trim() || '',
      startDate: this.newPhase.startDate || undefined,
      dueDate: this.newPhase.dueDate || undefined,
    };
    this.supervisionService.createPhase(this.projectId, payload).subscribe({
      next: () => {
        this.creatingPhase = false;
        this.newPhase.name = '';
        this.newPhase.description = '';
        this.newPhase.startDate = '';
        this.newPhase.dueDate = '';
        this.toast.success('Phase créée.');
        this.loadPhases();
      },
      error: (err: HttpErrorResponse) => {
        this.creatingPhase = false;
        this.toast.error(httpErrorMessage(err, 'Création de phase impossible.'));
      },
    });
  }

  startPhase(phase: ProjectPhase): void {
    this.startingPhaseId = phase.id;
    this.supervisionService.startPhase(this.projectId, phase.id).subscribe({
      next: () => {
        this.startingPhaseId = null;
        this.toast.success('Phase démarrée.');
        this.loadPhases();
      },
      error: (err: HttpErrorResponse) => {
        this.startingPhaseId = null;
        this.toast.error(httpErrorMessage(err, 'Démarrage impossible.'));
      },
    });
  }

  closeSelectedPhase(): void {
    if (!this.selectedPhaseId) return;
    this.closingPhase = true;
    this.supervisionService.closePhase(this.projectId, this.selectedPhaseId).subscribe({
      next: () => {
        this.closingPhase = false;
        this.toast.success('Phase clôturée et approuvée.');
        this.loadPhases();
      },
      error: (err: HttpErrorResponse) => {
        this.closingPhase = false;
        this.toast.error(httpErrorMessage(err, 'Clôture de phase impossible.'));
      },
    });
  }

  deletePhase(phase: ProjectPhase): void {
    if (phase.status !== 'PLANNED') {
      this.toast.show('Suppression autorisée seulement pour les phases PLANNED.', 'info');
      return;
    }
    this.deletingPhaseId = phase.id;
    this.supervisionService.deletePhase(this.projectId, phase.id).subscribe({
      next: () => {
        this.deletingPhaseId = null;
        if (this.selectedPhaseId === phase.id) {
          this.selectedPhaseId = null;
          this.deliverables = [];
          this.meetings = [];
        }
        this.toast.success('Phase supprimée.');
        this.loadPhases();
      },
      error: (err: HttpErrorResponse) => {
        this.deletingPhaseId = null;
        this.toast.error(
          httpErrorMessage(
            err,
            'Suppression impossible. Vérifiez que le backend expose DELETE /api/projects/{projectId}/phases/{phaseId}.'
          )
        );
      },
    });
  }

  createDeliverable(): void {
    if (!this.selectedPhaseId) return;
    if (!this.newDeliverable.title.trim()) {
      this.toast.error('Titre du livrable requis.');
      return;
    }
    this.creatingDeliverable = true;
    const payload: CreateDeliverableRequest = {
      title: this.newDeliverable.title.trim(),
      description: this.newDeliverable.description?.trim() || '',
      type: this.newDeliverable.type,
    };
    this.supervisionService.createDeliverable(this.projectId, this.selectedPhaseId, payload).subscribe({
      next: () => {
        this.creatingDeliverable = false;
        this.newDeliverable.title = '';
        this.newDeliverable.description = '';
        this.newDeliverable.type = 'DOC';
        this.toast.success('Livrable ajouté.');
        this.selectPhase(this.selectedPhaseId!);
      },
      error: (err: HttpErrorResponse) => {
        this.creatingDeliverable = false;
        this.toast.error(httpErrorMessage(err, 'Ajout du livrable impossible.'));
      },
    });
  }

  reviewDeliverable(deliverableId: number, status: DeliverableReviewStatus, comment = ''): void {
    if (!this.selectedPhaseId) return;
    this.reviewingDeliverableId = deliverableId;
    const payload: ReviewDeliverableRequest = {
      reviewStatus: status,
      reviewComment: comment || undefined,
    };
    this.supervisionService.reviewDeliverable(this.projectId, this.selectedPhaseId, deliverableId, payload).subscribe({
      next: () => {
        this.reviewingDeliverableId = null;
        this.toast.success('Review enregistrée.');
        this.selectPhase(this.selectedPhaseId!);
        this.loadPhases();
      },
      error: (err: HttpErrorResponse) => {
        this.reviewingDeliverableId = null;
        this.toast.error(httpErrorMessage(err, 'Review impossible.'));
      },
    });
  }

  createMeeting(): void {
    if (!this.selectedPhaseId) return;
    if (!this.newMeeting.meetingAt) {
      this.toast.error('Date/heure du meeting requise.');
      return;
    }
    this.creatingMeeting = true;
    const payload: CreatePhaseMeetingRequest = {
      meetingAt: new Date(this.newMeeting.meetingAt).toISOString(),
      agenda: this.newMeeting.agenda?.trim() || undefined,
      summary: this.newMeeting.summary?.trim() || undefined,
      decision: this.newMeeting.decision,
    };
    this.supervisionService.createMeeting(this.projectId, this.selectedPhaseId, payload).subscribe({
      next: (created) => {
        this.creatingMeeting = false;
        this.newMeeting.meetingAt = '';
        this.newMeeting.agenda = '';
        this.newMeeting.summary = '';
        this.newMeeting.decision = 'GO';
        this.toast.success('Meeting ajouté.');
        this.notifyFreelancersMeetingScheduled(created);
        this.selectPhase(this.selectedPhaseId!);
      },
      error: (err: HttpErrorResponse) => {
        this.creatingMeeting = false;
        this.toast.error(httpErrorMessage(err, 'Création meeting impossible.'));
      },
    });
  }

  nextPhaseOrder(): number {
    if (!this.phases.length) return 1;
    return Math.max(...this.phases.map((p) => p.phaseOrder)) + 1;
  }

  statusClass(status: ProjectPhaseStatus): string {
    const map: Record<ProjectPhaseStatus, string> = {
      PLANNED: 'bg-gray-100 text-gray-700',
      IN_PROGRESS: 'bg-blue-100 text-blue-700',
      IN_REVIEW: 'bg-violet-100 text-violet-700',
      APPROVED: 'bg-emerald-100 text-emerald-700',
      BLOCKED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  reviewClass(status: DeliverableReviewStatus): string {
    const map: Record<DeliverableReviewStatus, string> = {
      PENDING: 'bg-amber-100 text-amber-700',
      CHANGES_REQUESTED: 'bg-red-100 text-red-700',
      ACCEPTED: 'bg-emerald-100 text-emerald-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  private notifyFreelancersMeetingScheduled(created: PhaseMeeting): void {
    const phase = this.selectedPhase;
    if (!phase) return;
    this.candidatureService.listByProject(this.projectId, this.clientId).subscribe({
      next: (rows) => {
        const accepted = rows.filter((c) => String(c.status).toUpperCase() === 'ACCEPTED');
        if (!accepted.length) return;
        const title = this.project?.title?.trim() || `Projet #${this.projectId}`;
        this.notificationService.notifyPhaseMeetingScheduled(
          this.projectId,
          title,
          phase.name,
          created.meetingAt,
          accepted.map((c) => ({ freelancerId: c.freelancerId }))
        );
      },
      error: () => {},
    });
  }

  scheduleRiskLevelClass(level: string): string {
    const u = String(level || '').toUpperCase();
    if (u === 'AT_RISK') return 'bg-red-100 text-red-800';
    if (u === 'WATCH') return 'bg-amber-100 text-amber-800';
    return 'bg-emerald-100 text-emerald-800';
  }

  recommendationClass(r: DecisionCopilotRecommendation): string {
    const map: Record<DecisionCopilotRecommendation, string> = {
      NO_ACTION: 'bg-gray-100 text-gray-700',
      REQUEST_CHANGES: 'bg-amber-100 text-amber-700',
      ACCEPT_PHASE: 'bg-emerald-100 text-emerald-700',
      SPLIT_PHASE: 'bg-violet-100 text-violet-700',
      ESCALATE_RISK: 'bg-red-100 text-red-700',
    };
    return map[r] ?? 'bg-gray-100 text-gray-700';
  }
}
