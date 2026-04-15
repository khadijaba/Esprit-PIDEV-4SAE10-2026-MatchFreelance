import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subject, EMPTY, catchError, filter, map, switchMap, takeUntil } from 'rxjs';
import { ContractService } from '../../services/contract.service';
import { CandidatureService } from '../../services/candidature.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Contract, ContractStatus } from '../../models/contract.model';
import { normalizeContractFromApi } from '../../utils/contract-normalize';
import {
  FinancialSummary,
  ContractHealth,
  ContractAiBriefing,
  ExtraBudgetAiAnalysis,
} from '../../models/contract-advanced.model';
import { ChatComponent } from '../chat/chat.component';
import { SignatureModalComponent } from '../signature-modal/signature-modal.component';

@Component({
  selector: 'app-freelancer-contract-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ChatComponent, SignatureModalComponent],
  templateUrl: './freelancer-contract-detail.component.html',
})
export class FreelancerContractDetailComponent implements OnInit, OnDestroy {
  contract?: Contract;
  projectTitle = '';
  loading = true;
  auth = inject(AuthService);
  private readonly destroy$ = new Subject<void>();

  get freelancerId(): number {
    const id = this.auth.currentUserId();
    return id != null ? Number(id) : 2;
  }
  extraBudgetAmount = 0;
  extraBudgetReason = '';
  proposing = false;
  progressPercentEdit = 0;
  savingProgress = false;
  financialSummary: FinancialSummary | null = null;
  contractHealth: ContractHealth | null = null;
  loadingFinancial = false;
  loadingHealth = false;
  showFinancial = false;
  showHealth = false;
  extraBudgetAnalysis: ExtraBudgetAiAnalysis | null = null;
  loadingExtraBudgetAi = false;

  partyActionLoading = false;
  showFreelancerAmend = false;
  freelancerAmendTerms = '';
  freelancerAmendMessage = '';

  // Signature feature
  showSignatureModal = false;
  pendingDownloadContractId: number | null = null;

  constructor(
    private contractService: ContractService,
    private candidatureService: CandidatureService,
    private projectService: ProjectService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.route.paramMap
      .pipe(
        map((p) => +p.get('id')!),
        filter((id) => Number.isFinite(id) && id > 0),
        switchMap((id) => {
          this.loading = true;
          this.contract = undefined;
          this.showFreelancerAmend = false;
          this.extraBudgetAnalysis = null;
          return this.contractService.getById(id).pipe(
            catchError(() => {
              this.loading = false;
              this.toast.error('Contract not found');
              this.router.navigate(['/contracts']);
              return EMPTY;
            })
          );
        }),
        takeUntil(this.destroy$)
      )
      .subscribe((c) => {
        const me = this.auth.currentUserId();
        if (me == null) {
          this.loading = false;
          this.router.navigate(['/login']);
          return;
        }
        if (Number(c.freelancerId) !== Number(me)) {
          this.loading = false;
          this.toast.error('You do not have access to this contract');
          this.router.navigate(['/contracts']);
          return;
        }
        this.contract = normalizeContractFromApi(c);
        this.progressPercentEdit = this.contract.progressPercent ?? 0;
        this.loading = false;
        this.projectService.getById(this.contract.projectId).subscribe({
          next: (p) => (this.projectTitle = p.title),
          error: () => (this.projectTitle = ''),
        });
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Update progress from backend response when present, otherwise refetch contract. */
  onChatMessageSent(newProgress?: number) {
    if (newProgress != null && this.contract) {
      this.contract.progressPercent = newProgress;
      this.progressPercentEdit = newProgress;
    } else if (this.contract?.id) {
      this.contractService.getById(this.contract.id).subscribe({
        next: (c) => {
          this.contract = normalizeContractFromApi(c);
          this.progressPercentEdit = this.contract.progressPercent ?? 0;
        },
      });
    }
  }

  statusClass(status: ContractStatus): string {
    const map: Record<ContractStatus, string> = {
      DRAFT: 'bg-gray-100 text-gray-700',
      ACTIVE: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  formatStatus(status: ContractStatus): string {
    return String(status ?? '').replace(/_/g, ' ');
  }

  isActive(c: Contract): boolean {
    return c?.status != null && String(c.status).toUpperCase() === 'ACTIVE';
  }

  /** DRAFT/ACTIVE only — normalizes API quirks (whitespace, casing). */
  canPartyAct(c: Contract | null | undefined): boolean {
    if (!c) return false;
    const s = String(c.status ?? '')
      .trim()
      .toUpperCase();
    return s === 'DRAFT' || s === 'ACTIVE';
  }

  toggleFreelancerAmend() {
    if (!this.contract) return;
    this.showFreelancerAmend = !this.showFreelancerAmend;
    if (this.showFreelancerAmend) {
      this.freelancerAmendTerms = this.contract.terms ?? '';
      this.freelancerAmendMessage = this.contract.applicationMessage ?? '';
    }
  }

  onFreelancerCancelContract() {
    if (!this.contract || this.partyActionLoading) return;
    if (!this.canPartyAct(this.contract)) {
      this.toast.error('Only draft or active contracts can be cancelled');
      return;
    }
    if (!confirm('Cancel this contract? The project will reopen for the client and your assignment ends.')) return;
    this.partyActionLoading = true;
    const contractId = this.contract.id;
    this.candidatureService.cancelContractAsFreelancer(contractId, this.freelancerId).subscribe({
      next: () => {
        this.contractService.getById(contractId).subscribe({
          next: (c) => {
            this.contract = normalizeContractFromApi(c);
            this.partyActionLoading = false;
            this.showFreelancerAmend = false;
            this.toast.success('Contract cancelled. Project reopened for the client.');
          },
          error: () => {
            this.partyActionLoading = false;
            this.toast.success('Contract cancelled');
          },
        });
      },
      error: (err) => {
        this.partyActionLoading = false;
        this.toast.error(err?.error?.message || 'Failed to cancel contract');
      },
    });
  }

  saveFreelancerAmend() {
    if (!this.contract || this.partyActionLoading) return;
    if (!this.canPartyAct(this.contract)) {
      this.toast.error('Only draft or active contracts can be amended');
      return;
    }
    this.partyActionLoading = true;
    this.contractService
      .partyAmend(this.contract.id, {
        actorFreelancerId: this.freelancerId,
        applicationMessage: this.freelancerAmendMessage,
      })
      .subscribe({
        next: (c) => {
          this.contract = normalizeContractFromApi(c);
          this.partyActionLoading = false;
          this.showFreelancerAmend = false;
          this.toast.success('Contract updated');
        },
        error: (err) => {
          this.partyActionLoading = false;
          this.toast.error(err?.error?.message || 'Failed to update contract');
        },
      });
  }

  analyzeExtraBudgetWithAi() {
    if (!this.contract || this.loadingExtraBudgetAi) return;
    const amount = Number(this.extraBudgetAmount);
    if (!amount || amount <= 0) {
      this.toast.error('Enter a positive amount to analyze');
      return;
    }
    this.loadingExtraBudgetAi = true;
    this.extraBudgetAnalysis = null;
    this.contractService
      .analyzeExtraBudget(this.contract.id, {
        amount,
        reason: this.extraBudgetReason.trim() || undefined,
        freelancerId: this.freelancerId,
      })
      .subscribe({
        next: (data) => {
          this.extraBudgetAnalysis = data;
          this.loadingExtraBudgetAi = false;
        },
        error: (err) => {
          this.loadingExtraBudgetAi = false;
          this.toast.error(
            err?.error?.message || err?.message || 'AI analysis unavailable (enable Ollama on the server or try again).'
          );
        },
      });
  }

  proposeExtraBudget() {
    if (!this.contract || this.proposing) return;
    const amount = Number(this.extraBudgetAmount);
    if (!amount || amount <= 0) {
      this.toast.error('Enter a positive amount');
      return;
    }
    this.proposing = true;
    this.contractService
      .proposeExtraBudget(this.contract.id, amount, this.extraBudgetReason.trim(), this.freelancerId)
      .subscribe({
        next: (c) => {
          this.contract = normalizeContractFromApi(c);
          this.extraBudgetAmount = 0;
          this.extraBudgetReason = '';
          this.proposing = false;
          this.toast.success('Proposal sent to client');
        },
        error: (err) => {
          this.proposing = false;
          this.toast.error(err?.error?.message || 'Failed to send proposal');
        },
      });
  }

  saveProgress() {
    if (!this.contract || this.savingProgress) return;
    const value = Math.max(0, Math.min(100, Number(this.progressPercentEdit) || 0));
    this.savingProgress = true;
    this.contractService.updateProgress(this.contract.id, value, this.freelancerId).subscribe({
      next: (c) => {
        this.contract = normalizeContractFromApi(c);
        this.progressPercentEdit = this.contract.progressPercent ?? 0;
        this.savingProgress = false;
        this.toast.success('Progress updated');
      },
      error: (err) => {
        this.savingProgress = false;
        this.toast.error(err?.error?.message || 'Failed to update progress');
      },
    });
  }

  downloadPdf() {
    if (!this.contract) return;
    this.pendingDownloadContractId = this.contract.id;
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

  loadFinancialSummary() {
    if (!this.contract) return;
    this.showFinancial = !this.showFinancial;
    if (this.showFinancial && !this.financialSummary) {
      this.loadingFinancial = true;
      this.contractService.getFinancialSummary(this.contract.id).subscribe({
        next: (data) => {
          this.financialSummary = data;
          this.loadingFinancial = false;
        },
        error: () => {
          this.loadingFinancial = false;
        },
      });
    }
  }

  loadContractHealth() {
    if (!this.contract) return;
    this.showHealth = !this.showHealth;
    if (this.showHealth && !this.contractHealth) {
      this.loadingHealth = true;
      this.contractService.getContractHealth(this.contract.id).subscribe({
        next: (data) => {
          this.contractHealth = data;
          this.loadingHealth = false;
        },
        error: () => {
          this.loadingHealth = false;
        },
      });
    }
  }

  /** Pretty-print enum-style AI fields (e.g. NEEDS_SCOPE -> Needs scope). */
  formatAiLabel(value: string | undefined): string {
    if (!value) return '—';
    return value
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }
}
