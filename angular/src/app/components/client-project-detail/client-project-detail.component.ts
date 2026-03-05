import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService } from '../../services/contract.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus, ContractSummary } from '../../models/project.model';
import { Candidature, CandidatureStatus } from '../../models/candidature.model';
import { RankedCandidature, BudgetStats } from '../../models/ranking.model';
import { FinancialSummary, ContractHealth } from '../../models/contract-advanced.model';
import { TaskListComponent } from '../task-list/task-list.component';
import { InterviewScheduleComponent } from '../interview-schedule/interview-schedule.component';
import { SwipeCardComponent } from '../swipe-card/swipe-card.component';
import { ChatComponent } from '../chat/chat.component';
import { AuthService } from '../../services/auth.service';
import { analyzePitch, projectToPitchJob } from '../../services/pitch-analyzer.service';
import type { PitchAnalysisResult } from '../../services/pitch-analyzer.service';

@Component({
  selector: 'app-client-project-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TaskListComponent, InterviewScheduleComponent, SwipeCardComponent, ChatComponent],
  templateUrl: './client-project-detail.component.html',
})
export class ClientProjectDetailComponent implements OnInit {
  project?: Project;
  candidatures: Candidature[] = [];
  loading = true;
  auth = inject(AuthService);
  get clientId(): number {
    return this.auth.currentUserId() ?? 1;
  }
  animatingId: number | null = null;
  animatingDirection: 'accept' | 'reject' | null = null;
  contractActionLoading = false;
  respondingContractId: number | null = null;
  viewMode: 'swipe' | 'list' | 'ranked' = 'swipe';
  rankedCandidatures: RankedCandidature[] = [];
  budgetStats: BudgetStats | null = null;
  loadingRanked = false;
  loadingBudgetStats = false;
  ratingContractId: number | null = null;
  financialSummary: Record<number, FinancialSummary> = {};
  contractHealth: Record<number, ContractHealth> = {};
  loadingFinancial: number | null = null;
  loadingHealth: number | null = null;
  showFinancialId: number | null = null;
  showHealthId: number | null = null;
  ratingValue = 5;
  ratingReview = '';
  ratingSubmitting = false;
  /** Candidature id whose pitch analysis is currently shown. */
  analyzedCandidatureId: number | null = null;
  pitchAnalysisResult: PitchAnalysisResult | null = null;

  get pendingCandidatures(): Candidature[] {
    return this.candidatures.filter((c) => c.status === 'PENDING');
  }

  get nonPendingCandidatures(): Candidature[] {
    return this.candidatures.filter((c) => c.status !== 'PENDING');
  }

  get topPendingCard(): Candidature | undefined {
    return this.pendingCandidatures[0];
  }

  get backCards(): Candidature[] {
    return this.pendingCandidatures.slice(1, 3);
  }

  constructor(
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private contractService: ContractService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        if (p.clientId !== this.clientId) {
          this.toast.error('You are not the owner of this project');
          this.router.navigate(['/client']);
          return;
        }
        this.loadCandidatures(id);
        this.loadContractsIfNeeded(id);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Project not found');
        this.router.navigate(['/client']);
      },
    });
  }

  loadContractsIfNeeded(projectId: number) {
    if (!this.project || (this.project.status !== 'IN_PROGRESS' && this.project.status !== 'COMPLETED')) return;
    this.contractService.getByProjectId(projectId).subscribe({
      next: (contracts) => {
        if (this.project) {
          this.project.contracts = contracts.map((c) => ({
            id: c.id,
            projectId: c.projectId,
            freelancerId: c.freelancerId,
            clientId: c.clientId,
            terms: c.terms,
            proposedBudget: c.proposedBudget,
            extraTasksBudget: c.extraTasksBudget,
            applicationMessage: c.applicationMessage,
            pendingExtraAmount: c.pendingExtraAmount,
            pendingExtraReason: c.pendingExtraReason,
            pendingExtraRequestedAt: c.pendingExtraRequestedAt,
            progressPercent: c.progressPercent,
            status: c.status,
            startDate: c.startDate,
            endDate: c.endDate,
            createdAt: c.createdAt,
          }));
        }
      },
    });
  }

  loadCandidatures(projectId: number) {
    this.candidatureService.getByProjectId(projectId).subscribe({
      next: (data) => {
        this.candidatures = data;
        if (data.length > 0) {
          this.loadRankedCandidatures(projectId);
          this.loadBudgetStats(projectId);
        } else {
          this.rankedCandidatures = [];
          this.budgetStats = null;
        }
      },
    });
  }

  loadRankedCandidatures(projectId: number) {
    this.loadingRanked = true;
    this.candidatureService.getRankedByProjectId(projectId).subscribe({
      next: (data) => {
        this.rankedCandidatures = data;
        this.loadingRanked = false;
      },
      error: () => {
        this.loadingRanked = false;
      },
    });
  }

  loadBudgetStats(projectId: number) {
    this.loadingBudgetStats = true;
    this.candidatureService.getBudgetStatsByProjectId(projectId).subscribe({
      next: (data) => {
        this.budgetStats = data;
        this.loadingBudgetStats = false;
      },
      error: () => {
        this.loadingBudgetStats = false;
      },
    });
  }

  onAccept(c: Candidature) {
    this.candidatureService.accept(c.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Freelancer accepted');
        this.animatingId = c.id;
        this.animatingDirection = 'accept';
        setTimeout(() => {
          if (this.project) {
            this.loadCandidatures(this.project.id);
            this.projectService.getById(this.project.id).subscribe({
              next: (p) => {
                this.project = p;
                this.loadContractsIfNeeded(p.id);
              },
            });
          }
          this.animatingId = null;
          this.animatingDirection = null;
        }, 400);
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to accept'),
    });
  }

  onReject(c: Candidature) {
    this.candidatureService.reject(c.id).subscribe({
      next: () => {
        this.toast.success('Application rejected');
        this.animatingId = c.id;
        this.animatingDirection = 'reject';
        setTimeout(() => {
          if (this.project) this.loadCandidatures(this.project.id);
          this.animatingId = null;
          this.animatingDirection = null;
        }, 400);
      },
      error: () => this.toast.error('Failed to reject'),
    });
  }

  onPay(contract: ContractSummary) {
    this.contractActionLoading = true;
    this.candidatureService.payContract(contract.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Payment recorded. Project completed.');
        this.contractActionLoading = false;
        if (this.project) {
          this.projectService.getById(this.project.id).subscribe({
            next: (p) => {
              this.project = p;
              this.loadContractsIfNeeded(p.id);
            },
          });
        }
      },
      error: (err) => {
        this.contractActionLoading = false;
        this.toast.error(err?.error?.message || 'Failed to mark as paid');
      },
    });
  }

  onCancel(contract: ContractSummary) {
    if (!confirm('Cancel this contract and reopen the project to choose another freelancer?')) return;
    this.contractActionLoading = true;
    this.candidatureService.cancelContract(contract.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Contract cancelled. Project reopened.');
        this.contractActionLoading = false;
        if (this.project) {
          this.projectService.getById(this.project.id).subscribe({
            next: (p) => {
              this.project = p;
              this.loadCandidatures(p.id);
              this.loadContractsIfNeeded(p.id);
            },
          });
        }
      },
      error: (err) => {
        this.contractActionLoading = false;
        this.toast.error(err?.error?.message || 'Failed to cancel contract');
      },
    });
  }

  onSubmitRating(contract: ContractSummary) {
    if (!this.project) return;
    if (!this.ratingValue || this.ratingValue < 1 || this.ratingValue > 5) {
      this.toast.error('Please select a rating between 1 and 5');
      return;
    }
    if (contract.clientRating != null) {
      this.toast.error('You have already rated this contract');
      return;
    }
    this.ratingContractId = contract.id;
    this.ratingSubmitting = true;
    this.contractService
      .rateContract(contract.id, this.ratingValue, this.ratingReview?.trim() || undefined, this.clientId)
      .subscribe({
        next: (updated) => {
          const idx = this.project!.contracts?.findIndex((c) => c.id === contract.id) ?? -1;
          if (idx !== -1 && this.project!.contracts) {
            this.project!.contracts[idx] = {
              ...this.project!.contracts[idx],
              clientRating: updated.clientRating,
              clientReview: updated.clientReview,
              clientReviewedAt: updated.clientReviewedAt,
            };
          }
          this.ratingSubmitting = false;
          this.ratingContractId = null;
          this.toast.success('Thank you for your review');
        },
        error: (err) => {
          this.ratingSubmitting = false;
          this.ratingContractId = null;
          this.toast.error(err?.error?.message || 'Failed to submit review');
        },
      });
  }

  downloadContractPdf(contract: ContractSummary) {
    this.contractService.downloadPdf(contract.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contract-${contract.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.toast.error('Failed to download contract PDF');
      },
    });
  }

  statusClass(status: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      OPEN: 'bg-emerald-100 text-emerald-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  candidatureStatusClass(status: CandidatureStatus): string {
    const map: Record<CandidatureStatus, string> = {
      PENDING: 'bg-amber-100 text-amber-700',
      ACCEPTED: 'bg-emerald-100 text-emerald-700',
      REJECTED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  loadFinancialSummary(contractId: number) {
    if (this.financialSummary[contractId] != null) {
      this.showFinancialId = this.showFinancialId === contractId ? null : contractId;
      return;
    }
    this.loadingFinancial = contractId;
    this.contractService.getFinancialSummary(contractId).subscribe({
      next: (data) => {
        this.financialSummary[contractId] = data;
        this.showFinancialId = contractId;
        this.loadingFinancial = null;
      },
      error: () => {
        this.loadingFinancial = null;
      },
    });
  }

  loadContractHealth(contractId: number) {
    if (this.contractHealth[contractId] != null) {
      this.showHealthId = this.showHealthId === contractId ? null : contractId;
      return;
    }
    this.loadingHealth = contractId;
    this.contractService.getContractHealth(contractId).subscribe({
      next: (data) => {
        this.contractHealth[contractId] = data;
        this.showHealthId = contractId;
        this.loadingHealth = null;
      },
      error: () => {
        this.loadingHealth = null;
      },
    });
  }

  /** Run local pitch analysis for this freelancer's application message so the client can review before accepting. */
  analyzePitchFor(c: Candidature) {
    if (!this.project) return;
    const pitch = c.message ?? '';
    if (pitch.trim().length < 10) {
      this.toast.error('This application has no message to analyze.');
      return;
    }
    const job = projectToPitchJob(this.project);
    this.pitchAnalysisResult = analyzePitch(job, pitch);
    this.analyzedCandidatureId = c.id;
  }

  closePitchAnalysis() {
    this.analyzedCandidatureId = null;
    this.pitchAnalysisResult = null;
  }

  pitchScoreColor(score: number): string {
    return score >= 80 ? '#059669' : score >= 60 ? '#0d9488' : score >= 40 ? '#d97706' : '#dc2626';
  }

  pitchScoreLabel(score: number): string {
    return score >= 80 ? 'Excellent' : score >= 60 ? 'Good' : score >= 40 ? 'Partial' : 'Weak';
  }

  /** Update progress bar when backend detected progress from chat, or refetch contracts. */
  onChatMessageSent(contract: ContractSummary, newProgress?: number) {
    if (newProgress != null && this.project?.contracts) {
      const c = this.project.contracts.find((x) => x.id === contract.id);
      if (c) c.progressPercent = newProgress;
    } else if (this.project) {
      this.loadContractsIfNeeded(this.project.id);
    }
  }

  onRespondToExtraBudget(contract: ContractSummary, accept: boolean) {
    if (!this.project?.contracts || this.respondingContractId != null) return;
    this.respondingContractId = contract.id;
    this.contractService.respondToExtraBudget(contract.id, accept, contract.clientId).subscribe({
      next: (updated) => {
        const idx = this.project!.contracts!.findIndex((c) => c.id === contract.id);
        if (idx !== -1) {
          this.project!.contracts![idx] = {
            id: updated.id,
            projectId: updated.projectId,
            freelancerId: updated.freelancerId,
            clientId: updated.clientId,
            terms: updated.terms,
            proposedBudget: updated.proposedBudget,
            extraTasksBudget: updated.extraTasksBudget,
            applicationMessage: updated.applicationMessage,
            status: updated.status,
            startDate: updated.startDate,
            endDate: updated.endDate,
            createdAt: updated.createdAt,
            pendingExtraAmount: updated.pendingExtraAmount,
            pendingExtraReason: updated.pendingExtraReason,
            pendingExtraRequestedAt: updated.pendingExtraRequestedAt,
            progressPercent: updated.progressPercent,
          };
        }
        this.respondingContractId = null;
        this.toast.success(accept ? 'Extra budget accepted' : 'Proposal declined');
      },
      error: (err) => {
        this.respondingContractId = null;
        this.toast.error(err?.error?.message || 'Failed to respond');
      },
    });
  }
}
