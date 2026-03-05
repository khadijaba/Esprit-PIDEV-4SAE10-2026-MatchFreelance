import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ContractService } from '../../services/contract.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Contract, ContractStatus } from '../../models/contract.model';
import { FinancialSummary, ContractHealth } from '../../models/contract-advanced.model';
import { ChatComponent } from '../chat/chat.component';

@Component({
  selector: 'app-freelancer-contract-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ChatComponent],
  templateUrl: './freelancer-contract-detail.component.html',
})
export class FreelancerContractDetailComponent implements OnInit {
  contract?: Contract;
  projectTitle = '';
  loading = true;
  auth = inject(AuthService);
  get freelancerId(): number {
    return this.auth.currentUserId() ?? 2;
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

  constructor(
    private contractService: ContractService,
    private projectService: ProjectService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.contractService.getById(id).subscribe({
      next: (c) => {
        if (c.freelancerId !== this.freelancerId) {
          this.toast.error('You do not have access to this contract');
          this.router.navigate(['/contracts']);
          return;
        }
        this.contract = c;
        this.progressPercentEdit = c.progressPercent ?? 0;
        this.loading = false;
        this.projectService.getById(c.projectId).subscribe({
          next: (p) => (this.projectTitle = p.title),
          error: () => (this.projectTitle = ''),
        });
      },
      error: () => {
        this.loading = false;
        this.toast.error('Contract not found');
        this.router.navigate(['/contracts']);
      },
    });
  }

  /** Update progress from backend response when present, otherwise refetch contract. */
  onChatMessageSent(newProgress?: number) {
    if (newProgress != null && this.contract) {
      this.contract.progressPercent = newProgress;
      this.progressPercentEdit = newProgress;
    } else if (this.contract?.id) {
      this.contractService.getById(this.contract.id).subscribe({
        next: (c) => {
          this.contract = c;
          this.progressPercentEdit = c.progressPercent ?? 0;
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
    return status.replace('_', ' ');
  }

  isActive(c: Contract): boolean {
    return c?.status != null && String(c.status).toUpperCase() === 'ACTIVE';
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
          this.contract = c;
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
        this.contract = c;
        this.progressPercentEdit = c.progressPercent ?? 0;
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
    this.contractService.downloadPdf(this.contract.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contract-${this.contract!.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.toast.error('Failed to download contract PDF');
      },
    });
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
}
