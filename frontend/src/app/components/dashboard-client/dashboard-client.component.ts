import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService } from '../../services/contract.service';
import { ToastService } from '../../services/toast.service';
import { Project } from '../../models/project.model';
import { CandidatureResponse } from '../../models/candidature.model';
import { ContractSummary } from '../../models/contract.model';
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
import { ContractChatPanelComponent } from '../contract-chat-panel/contract-chat-panel.component';

@Component({
  selector: 'app-dashboard-client',
  standalone: true,
  imports: [CommonModule, RouterLink, ContractChatPanelComponent],
  templateUrl: './dashboard-client.component.html',
})
export class DashboardClientComponent implements OnInit {
  projects: Project[] = [];
  applicationCounts: Record<number, number> = {};
  contractCounts: Record<number, number> = {};
  selectedProjectId: number | null = null;
  selectedApplications: CandidatureResponse[] = [];
  selectedContracts: ContractSummary[] = [];
  loadingProjectPanel = false;
  loading = true;
  error: string | null = null;
  actionLoading = false;
  selectedApplicationView: 'swipe' | 'list' | 'ranked' = 'list';
  swipeIndex = 0;
  analyzedPitchCandidatureId: number | null = null;
  pitchAnalysisResult: PitchAnalysisResult | null = null;

  constructor(
    public auth: AuthService,
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private contractService: ContractService,
    private toast: ToastService
  ) {}

  get clientId(): number | null {
    return this.auth.getStoredUser()?.userId ?? null;
  }

  ngOnInit() {
    if (!this.auth.isLoggedIn() || !this.auth.isProjectOwner()) {
      this.error = 'Connectez-vous en tant que Client pour accéder à ce tableau de bord.';
      this.loading = false;
      return;
    }
    this.loadDashboard();
  }

  loadDashboard(): void {
    const cid = this.clientId;
    if (!cid) {
      this.error = 'Utilisateur owner invalide.';
      this.loading = false;
      return;
    }
    this.loading = true;
    this.projectService.getByOwnerId(cid).subscribe({
      next: (rows) => {
        this.projects = (rows ?? []).sort((a, b) => b.id - a.id);
        this.loading = false;
        this.loadCounters(cid);
        if (this.projects.length > 0) {
          this.selectProject(this.projects[0].id);
        } else {
          this.selectedProjectId = null;
          this.selectedApplications = [];
          this.selectedContracts = [];
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Impossible de charger vos projets.';
      },
    });
  }

  private loadCounters(clientId: number): void {
    this.projects.forEach((p) => {
      this.candidatureService.listByProject(p.id, clientId).subscribe({
        next: (apps) => {
          this.applicationCounts[p.id] = apps?.length ?? 0;
        },
        error: () => {
          this.applicationCounts[p.id] = 0;
        },
      });
      this.contractService.listByProject(p.id).subscribe({
        next: (contracts) => {
          this.contractCounts[p.id] = contracts?.length ?? 0;
        },
        error: () => {
          this.contractCounts[p.id] = 0;
        },
      });
    });
  }

  selectProject(projectId: number): void {
    const cid = this.clientId;
    if (!cid) return;
    this.selectedProjectId = projectId;
    this.swipeIndex = 0;
    this.analyzedPitchCandidatureId = null;
    this.pitchAnalysisResult = null;
    this.loadingProjectPanel = true;
    this.candidatureService.listByProject(projectId, cid).subscribe({
      next: (apps) => {
        this.selectedApplications = apps ?? [];
        this.clampSwipeIndex();
        this.loadingProjectPanel = false;
      },
      error: () => {
        this.selectedApplications = [];
        this.clampSwipeIndex();
        this.loadingProjectPanel = false;
      },
    });
    this.contractService.listByProject(projectId).subscribe({
      next: (contracts) => {
        this.selectedContracts = contracts ?? [];
      },
      error: () => {
        this.selectedContracts = [];
      },
    });
  }

  setApplicationView(view: 'swipe' | 'list' | 'ranked'): void {
    this.selectedApplicationView = view;
    this.swipeIndex = 0;
    this.analyzedPitchCandidatureId = null;
    this.pitchAnalysisResult = null;
  }

  get swipeQueue(): CandidatureResponse[] {
    return this.selectedApplications.filter((c) => c.status === 'PENDING');
  }

  get swipeCurrent(): CandidatureResponse | null {
    const q = this.swipeQueue;
    if (q.length === 0 || this.swipeIndex < 0 || this.swipeIndex >= q.length) return null;
    return q[this.swipeIndex];
  }

  get rankedApplications(): CandidatureResponse[] {
    return [...this.selectedApplications].sort((a, b) => {
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

  skillTagsForProject(): string[] {
    const p = this.projects.find((x) => x.id === this.selectedProjectId);
    const s = p?.requiredSkills;
    return s?.length ? s.slice(0, 6) : [];
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

  private clampSwipeIndex(): void {
    const n = this.swipeQueue.length;
    if (n === 0) this.swipeIndex = 0;
    else if (this.swipeIndex >= n) this.swipeIndex = n - 1;
  }

  acceptApplication(app: CandidatureResponse): void {
    const cid = this.clientId;
    if (!cid || this.selectedProjectId == null) return;
    this.actionLoading = true;
    this.candidatureService.accept(app.id, cid).subscribe({
      next: () => {
        this.toast.success('Candidature acceptee.');
        this.actionLoading = false;
        this.loadDashboard();
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Echec acceptation.');
        this.actionLoading = false;
      },
    });
  }

  rejectApplication(app: CandidatureResponse): void {
    const cid = this.clientId;
    if (!cid || this.selectedProjectId == null) return;
    this.actionLoading = true;
    this.candidatureService.reject(app.id, cid).subscribe({
      next: () => {
        this.toast.success('Candidature rejetee.');
        this.actionLoading = false;
        this.selectProject(this.selectedProjectId!);
        this.loadCounters(cid);
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Echec rejet.');
        this.actionLoading = false;
      },
    });
  }

  analyzePitchFor(app: CandidatureResponse): void {
    const p = this.projects.find((x) => x.id === this.selectedProjectId);
    if (!p) return;
    const pitch = app.message ?? '';
    if (pitch.trim().length < 10) {
      this.toast.error("Message de candidature trop court pour l'analyse.");
      return;
    }
    const job = matchfreelanceProjectToPitchJob(p);
    this.pitchAnalysisResult = runPitchAnalysis(job, pitch);
    this.analyzedPitchCandidatureId = app.id;
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

  payContract(contract: ContractSummary): void {
    const cid = this.clientId;
    if (!cid || this.selectedProjectId == null) return;
    this.actionLoading = true;
    this.candidatureService.payContract(contract.id, cid).subscribe({
      next: () => {
        this.toast.success('Paiement effectue.');
        this.actionLoading = false;
        this.loadDashboard();
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Echec paiement.');
        this.actionLoading = false;
      },
    });
  }

  cancelContract(contract: ContractSummary): void {
    const cid = this.clientId;
    if (!cid || this.selectedProjectId == null) return;
    this.actionLoading = true;
    this.candidatureService.cancelContract(contract.id, cid).subscribe({
      next: () => {
        this.toast.success('Contrat annule.');
        this.actionLoading = false;
        this.loadDashboard();
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Echec annulation contrat.');
        this.actionLoading = false;
      },
    });
  }

  openContractPdf(contractId: number): void {
    window.open(`/api/contracts/${contractId}/pdf`, '_blank');
  }

  isPendingCandidature(c: CandidatureResponse): boolean {
    return isCandidaturePending(c.status);
  }

  canAccept(app: CandidatureResponse): boolean {
    return canOwnerAcceptCandidature(app);
  }

  canReject(app: CandidatureResponse): boolean {
    return canOwnerRejectCandidature(app);
  }

  acceptDisabledTitle(app: CandidatureResponse): string {
    if (!this.isPendingCandidature(app)) return '';
    if (!canOwnerAcceptCandidature(app)) {
      return 'Marquez au moins un entretien comme terminé (COMPLETED) via « Gérer entretiens », puis réessayez.';
    }
    return '';
  }
}
