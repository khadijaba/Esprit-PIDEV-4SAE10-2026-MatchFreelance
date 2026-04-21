import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService } from '../../services/contract.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Project } from '../../models/project.model';
import { CandidatureResponse } from '../../models/candidature.model';
import { ContractSummary } from '../../models/contract.model';
import { InterviewScheduleComponent } from '../interview-schedule/interview-schedule.component';
import { ContractChatPanelComponent } from '../contract-chat-panel/contract-chat-panel.component';
import {
  analyzePitch as runPitchAnalysis,
  matchfreelanceProjectToPitchJob,
  type PitchAnalysisResult,
} from '../../services/pitch-analyzer.service';
import {
  canOwnerAcceptCandidature,
  canOwnerRejectCandidature,
  isCandidaturePending,
} from '../../utils/candidature-status';

@Component({
  selector: 'app-project-recrutement',
  standalone: true,
  imports: [CommonModule, RouterLink, InterviewScheduleComponent, ContractChatPanelComponent],
  templateUrl: './project-recrutement.component.html',
})
export class ProjectRecrutementComponent implements OnInit {
  projectId!: number;
  project: Project | null = null;
  candidatures: CandidatureResponse[] = [];
  contracts: ContractSummary[] = [];
  openedInterviewCandidatureId: number | null = null;
  clientId: number | null = null;
  loading = true;
  error: string | null = null;
  actionLoading = false;
  applicationView: 'swipe' | 'list' | 'ranked' = 'list';
  swipeIndex = 0;
  analyzedPitchCandidatureId: number | null = null;
  pitchAnalysisResult: PitchAnalysisResult | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private contractService: ContractService,
    private toast: ToastService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.router.navigate(['/projets']);
      return;
    }
    this.projectId = id;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        const me = this.auth.getStoredUser();
        this.clientId = me?.userId ?? null;
        if (!this.clientId || p.projectOwnerId !== this.clientId) {
          this.error = 'Réservé au porteur du projet.';
          this.loading = false;
          return;
        }
        this.loadLists();
        this.loading = false;
      },
      error: () => {
        this.error = 'Projet introuvable.';
        this.loading = false;
      },
    });
  }

  private loadLists(): void {
    if (!this.clientId) {
      this.candidatures = [];
      return;
    }
    this.candidatureService.listByProject(this.projectId, this.clientId).subscribe({
      next: (c) => {
        this.candidatures = c ?? [];
        this.clampSwipeIndex();
      },
      error: () => {
        this.candidatures = [];
        this.clampSwipeIndex();
      },
    });
    this.contractService.listByProject(this.projectId).subscribe({
      next: (c) => (this.contracts = c ?? []),
      error: () => (this.contracts = []),
    });
  }

  toggleInterview(candidatureId: number): void {
    this.openedInterviewCandidatureId =
      this.openedInterviewCandidatureId === candidatureId ? null : candidatureId;
  }

  setApplicationView(view: 'swipe' | 'list' | 'ranked'): void {
    this.applicationView = view;
    this.swipeIndex = 0;
    this.analyzedPitchCandidatureId = null;
    this.pitchAnalysisResult = null;
  }

  /** Candidatures encore décidables (vue Swipe). */
  get swipeQueue(): CandidatureResponse[] {
    return this.candidatures.filter((c) => c.status === 'PENDING');
  }

  get swipeCurrent(): CandidatureResponse | null {
    const q = this.swipeQueue;
    if (q.length === 0 || this.swipeIndex < 0 || this.swipeIndex >= q.length) return null;
    return q[this.swipeIndex];
  }

  get rankedCandidatures(): CandidatureResponse[] {
    return [...this.candidatures].sort((a, b) => {
      const sa = a.aiMatchScore ?? -1;
      const sb = b.aiMatchScore ?? -1;
      if (sb !== sa) return sb - sa;
      return (b.proposedBudget ?? 0) - (a.proposedBudget ?? 0);
    });
  }

  swipePrev(): void {
    if (this.swipeIndex > 0) this.swipeIndex--;
  }

  swipeNext(): void {
    const max = this.swipeQueue.length - 1;
    if (this.swipeIndex < max) this.swipeIndex++;
  }

  chatPeerLabel(contract: ContractSummary): string {
    return contract.freelancerName?.trim() || `Freelancer #${contract.freelancerId ?? '—'}`;
  }

  skillTags(): string[] {
    const s = this.project?.requiredSkills;
    return s?.length ? s.slice(0, 6) : [];
  }

  analyzePitchFor(c: CandidatureResponse): void {
    if (!this.project) return;
    const pitch = c.message ?? '';
    if (pitch.trim().length < 10) {
      this.toast.error("Message de candidature trop court pour l'analyse.");
      return;
    }
    const job = matchfreelanceProjectToPitchJob(this.project);
    this.pitchAnalysisResult = runPitchAnalysis(job, pitch);
    this.analyzedPitchCandidatureId = c.id;
  }

  closePitchAnalysis(): void {
    this.analyzedPitchCandidatureId = null;
    this.pitchAnalysisResult = null;
  }

  pitchScoreColor(score: number): string {
    return score >= 80 ? '#059669' : score >= 60 ? '#0d9488' : score >= 40 ? '#d97706' : '#dc2626';
  }

  pitchScoreLabel(score: number): string {
    return score >= 80 ? 'Excellent' : score >= 60 ? 'Good' : score >= 40 ? 'Partial' : 'Weak';
  }

  accept(c: CandidatureResponse): void {
    if (!this.clientId) return;
    this.actionLoading = true;
    this.candidatureService.accept(c.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Candidature acceptee.');
        this.actionLoading = false;
        this.loadLists();
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Echec acceptation candidature.');
        this.actionLoading = false;
      },
    });
  }

  reject(c: CandidatureResponse): void {
    if (!this.clientId) return;
    this.actionLoading = true;
    this.candidatureService.reject(c.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Candidature rejetee.');
        this.actionLoading = false;
        this.loadLists();
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Echec rejet candidature.');
        this.actionLoading = false;
      },
    });
  }

  payContract(c: ContractSummary): void {
    if (!this.clientId) return;
    this.actionLoading = true;
    this.candidatureService.payContract(c.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Paiement effectue.');
        this.actionLoading = false;
        this.loadLists();
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Echec paiement contrat.');
        this.actionLoading = false;
      },
    });
  }

  cancelContract(c: ContractSummary): void {
    if (!this.clientId) return;
    this.actionLoading = true;
    this.candidatureService.cancelContract(c.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Contrat annule.');
        this.actionLoading = false;
        this.loadLists();
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Echec annulation contrat.');
        this.actionLoading = false;
      },
    });
  }

  downloadPdf(c: ContractSummary): void {
    window.open(`/api/contracts/${c.id}/pdf`, '_blank');
  }

  private clampSwipeIndex(): void {
    const n = this.swipeQueue.length;
    if (n === 0) this.swipeIndex = 0;
    else if (this.swipeIndex >= n) this.swipeIndex = n - 1;
  }

  isPendingCandidature(c: CandidatureResponse): boolean {
    return isCandidaturePending(c.status);
  }

  canAccept(c: CandidatureResponse): boolean {
    return canOwnerAcceptCandidature(c);
  }

  canReject(c: CandidatureResponse): boolean {
    return canOwnerRejectCandidature(c);
  }

  acceptDisabledTitle(c: CandidatureResponse): string {
    if (!this.isPendingCandidature(c)) return '';
    if (!canOwnerAcceptCandidature(c)) {
      return 'Marquez au moins un entretien comme terminé (COMPLETED) via « Gérer entretiens », puis réessayez.';
    }
    return '';
  }

  contractDateRange(c: ContractSummary): string {
    const fmt = (s?: string) => {
      if (!s) return null;
      const d = new Date(s);
      if (Number.isNaN(d.getTime())) return null;
      return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short' }).format(d);
    };
    const a = fmt(c.startDate);
    const b = fmt(c.endDate);
    if (a && b) return `${a} — ${b}`;
    return a || b || '—';
  }
}
