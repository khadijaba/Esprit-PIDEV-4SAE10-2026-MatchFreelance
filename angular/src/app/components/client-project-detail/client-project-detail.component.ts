import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService, ContractPartyAmendRequest } from '../../services/contract.service';
import { Contract } from '../../models/contract.model';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus, ContractSummary } from '../../models/project.model';
import { Candidature, CandidatureStatus } from '../../models/candidature.model';
import { RankedCandidature, BudgetStats } from '../../models/ranking.model';
import { FinancialSummary, ContractHealth, ContractAiBriefing } from '../../models/contract-advanced.model';
import { TaskListComponent } from '../task-list/task-list.component';
import { InterviewScheduleComponent } from '../interview-schedule/interview-schedule.component';
import { SwipeCardComponent } from '../swipe-card/swipe-card.component';
import { ChatComponent } from '../chat/chat.component';
import { AuthService } from '../../services/auth.service';
import { analyzePitch, projectToPitchJob } from '../../services/pitch-analyzer.service';
import type { PitchAnalysisResult } from '../../services/pitch-analyzer.service';
import { ContractPreviewService, PreviewResponse } from '../../services/contract-preview.service';
import { PreviewModalComponent } from '../preview-modal/preview-modal.component';
import { SignatureModalComponent } from '../signature-modal/signature-modal.component';

@Component({
  selector: 'app-client-project-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, TaskListComponent, InterviewScheduleComponent, SwipeCardComponent, ChatComponent, PreviewModalComponent, SignatureModalComponent],
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
  financialSummary: Record<number, FinancialSummary> = {};
  contractHealth: Record<number, ContractHealth> = {};
  loadingFinancial: number | null = null;
  loadingHealth: number | null = null;
  showFinancialId: number | null = null;
  showHealthId: number | null = null;
  aiBriefingByContract: Record<number, ContractAiBriefing> = {};
  loadingAiBriefing: number | null = null;
  showAiBriefingId: number | null = null;
  /** Candidature id whose pitch analysis is currently shown. */
  analyzedCandidatureId: number | null = null;
  pitchAnalysisResult: PitchAnalysisResult | null = null;

  // Preview feature
  previewService = inject(ContractPreviewService);
  showPreviewModal = signal(false);
  currentPreview = signal<PreviewResponse | null>(null);
  generatingPreview = signal(false);

  // Signature feature
  showSignatureModal = false;
  pendingDownloadContractId: number | null = null;

  amendOpenForId: number | null = null;
  amendTerms = '';
  amendProposedBudget: number | null = null;
  amendEndDate = '';
  amendSaving = false;

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
        // Filter out rejected candidatures (including cancelled freelancers)
        this.candidatures = data.filter((c) => c.status !== 'REJECTED');
        if (this.candidatures.length > 0) {
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

  isDraftOrActive(contract: ContractSummary): boolean {
    const s = (contract.status || '').toUpperCase();
    return s === 'DRAFT' || s === 'ACTIVE';
  }

  toggleClientAmend(c: ContractSummary) {
    if (this.amendOpenForId === c.id) {
      this.amendOpenForId = null;
      return;
    }
    this.amendOpenForId = c.id;
    // Use project description instead of old contract terms
    if (this.project) {
      let terms = 'Project: ' + this.project.title;
      if (this.project.description) {
        terms += '\n\nDescription: ' + this.project.description;
      }
      if (c.proposedBudget != null) {
        terms += '\n\nBudget: ' + c.proposedBudget + ' TND';
      }
      if (this.project.duration) {
        terms += '\nDuration: ' + this.project.duration + ' days';
      }
      this.amendTerms = terms;
    } else {
      this.amendTerms = c.terms ?? '';
    }
    this.amendProposedBudget = c.proposedBudget ?? null;
    if (c.endDate) {
      const d = new Date(c.endDate);
      this.amendEndDate = !isNaN(d.getTime()) ? d.toISOString().slice(0, 10) : '';
    } else {
      this.amendEndDate = '';
    }
  }

  saveClientAmend(c: ContractSummary) {
    if (!this.project?.contracts) return;
    this.amendSaving = true;
    const body: ContractPartyAmendRequest = {
      actorClientId: this.clientId,
      terms: this.amendTerms,
    };
    if (this.amendProposedBudget != null && !Number.isNaN(Number(this.amendProposedBudget))) {
      body.proposedBudget = Number(this.amendProposedBudget);
    }
    if (this.amendEndDate?.trim()) {
      body.endDate = new Date(this.amendEndDate.trim() + 'T12:00:00').toISOString();
    }
    this.contractService.partyAmend(c.id, body).subscribe({
      next: (updated) => {
        this.patchContractSummaryFromResponse(c.id, updated);
        this.amendSaving = false;
        this.amendOpenForId = null;
        this.toast.success('Contract updated');
      },
      error: (err) => {
        this.amendSaving = false;
        this.toast.error(err?.error?.message || 'Failed to update contract');
      },
    });
  }

  private patchContractSummaryFromResponse(contractId: number, updated: Contract) {
    if (!this.project?.contracts) return;
    const idx = this.project.contracts.findIndex((x) => x.id === contractId);
    if (idx === -1) return;
    const prev = this.project.contracts[idx];
    this.project.contracts[idx] = {
      ...prev,
      terms: updated.terms,
      proposedBudget: updated.proposedBudget,
      extraTasksBudget: updated.extraTasksBudget,
      applicationMessage: updated.applicationMessage,
      status: updated.status as string,
      startDate: updated.startDate,
      endDate: updated.endDate,
      pendingExtraAmount: updated.pendingExtraAmount,
      pendingExtraReason: updated.pendingExtraReason,
      pendingExtraRequestedAt: updated.pendingExtraRequestedAt,
      progressPercent: updated.progressPercent,
    };
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

  downloadContractPdf(contract: ContractSummary) {
    this.pendingDownloadContractId = contract.id;
    this.showSignatureModal = true;
  }

  onSignatureSigned(signature: string) {
    if (this.pendingDownloadContractId === null) return;
    const contractId = this.pendingDownloadContractId;
    this.showSignatureModal = false;
    this.contractService.downloadPdf(contractId, signature).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contract-${contractId}-signed.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.pendingDownloadContractId = null;
      },
      error: () => {
        this.toast.error('Failed to download contract PDF');
        this.pendingDownloadContractId = null;
      },
    });
  }

  onSignatureCancelled() {
    this.showSignatureModal = false;
    this.pendingDownloadContractId = null;
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

  loadAiBriefing(contractId: number) {
    if (this.aiBriefingByContract[contractId] != null) {
      this.showAiBriefingId = this.showAiBriefingId === contractId ? null : contractId;
      return;
    }
    this.loadingAiBriefing = contractId;
    this.contractService.getAiBriefing(contractId, { viewerClientId: this.clientId }).subscribe({
      next: (data) => {
        this.aiBriefingByContract[contractId] = data;
        this.showAiBriefingId = contractId;
        this.loadingAiBriefing = null;
      },
      error: (err) => {
        this.loadingAiBriefing = null;
        this.toast.error(err?.error?.message || err?.message || 'AI briefing unavailable (enable Ollama on the server or try again).');
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

  // Preview feature methods
  async generatePreview(contractId: number) {
    this.generatingPreview.set(true);
    try {
      const preview = await this.previewService.generatePreview(contractId, 'modern');
      this.currentPreview.set(preview);
      this.showPreviewModal.set(true);
      this.toast.success('Preview generated!');
    } catch (error) {
      console.error('Preview generation failed:', error);
      this.toast.error('Failed to generate preview. Please try again.');
    } finally {
      this.generatingPreview.set(false);
    }
  }

  async approvePreview() {
    const preview = this.currentPreview();
    if (!preview) return;
    
    try {
      await this.previewService.submitFeedback(
        preview.contractId,
        preview.previewId,
        { feedback: 'Approved by client', status: 'APPROVED' }
      );
      this.toast.success('Preview approved!');
      this.showPreviewModal.set(false);
    } catch (error) {
      this.toast.error('Failed to approve preview');
    }
  }

  async handlePreviewFeedback(feedback: string) {
    const preview = this.currentPreview();
    if (!preview) return;
    
    try {
      await this.previewService.submitFeedback(
        preview.contractId,
        preview.previewId,
        { feedback, status: 'REVISION_REQUESTED' }
      );
      this.toast.success('Feedback submitted');
    } catch (error) {
      this.toast.error('Failed to submit feedback');
    }
  }

  async handlePreviewRegenerate(data: { feedback: string; style: string }) {
    const preview = this.currentPreview();
    if (!preview) return;
    
    this.generatingPreview.set(true);
    try {
      const newPreview = await this.previewService.regeneratePreview(
        preview.contractId,
        preview.previewId,
        data
      );
      this.currentPreview.set(newPreview);
      this.toast.success('Preview regenerated with your feedback!');
    } catch (error) {
      this.toast.error('Failed to regenerate preview');
    } finally {
      this.generatingPreview.set(false);
    }
  }
}
