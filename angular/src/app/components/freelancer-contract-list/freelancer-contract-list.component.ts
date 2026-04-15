import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of, catchError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ContractService } from '../../services/contract.service';
import { CandidatureService } from '../../services/candidature.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Contract, ContractStatus } from '../../models/contract.model';
import { Project } from '../../models/project.model';
import { normalizeContractFromApi } from '../../utils/contract-normalize';

@Component({
  selector: 'app-freelancer-contract-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './freelancer-contract-list.component.html',
})
export class FreelancerContractListComponent implements OnInit {
  contracts: Contract[] = [];
  projectTitles: Record<number, string> = {};
  loading = true;
  loadError: string | null = null;
  cancelingId: number | null = null;

  private readonly auth = inject(AuthService);

  get freelancerId(): number {
    const id = this.auth.currentUserId();
    return id != null ? Number(id) : 2;
  }

  /** Mirrors client project detail: disable Amend/Cancel while a cancel request is in flight. */
  get contractActionLoading(): boolean {
    return this.cancelingId != null;
  }

  constructor(
    private contractService: ContractService,
    private candidatureService: CandidatureService,
    private projectService: ProjectService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  trackByContractId(_index: number, c: Contract): number {
    return c.id;
  }

  load(): void {
    this.loading = true;
    this.loadError = null;
    this.contractService.getByFreelancerId(this.freelancerId).subscribe({
      next: (data) => {
        // Filter out cancelled contracts
        this.contracts = (data ?? [])
          .map((c) => normalizeContractFromApi(c))
          .filter((c) => c.status !== 'CANCELLED');
        this.loading = false;

        const projectIds = [...new Set(this.contracts.map((c) => c.projectId))];
        if (projectIds.length === 0) return;

        forkJoin(
          projectIds.map((id) =>
            this.projectService.getById(id).pipe(catchError(() => of(null as Project | null)))
          )
        ).subscribe({
          next: (projects) => {
            this.projectTitles = {};
            projects.forEach((p, i) => {
              const pid = projectIds[i];
              if (p?.title) this.projectTitles[pid] = p.title;
            });
          },
        });
      },
      error: (err: unknown) => {
        this.loading = false;
        this.contracts = [];
        if (err instanceof HttpErrorResponse) {
          const body = err.error as { message?: string } | string | null;
          const detail =
            typeof body === 'object' && body && 'message' in body
              ? String(body.message)
              : typeof body === 'string'
                ? body
                : '';
          this.loadError = [err.status, err.statusText, detail].filter(Boolean).join(' — ') || 'HTTP error';
        } else {
          this.loadError = 'Request failed';
        }
        this.toast.error('Failed to load contracts');
      },
    });
  }

  downloadPdf(contractId: number): void {
    this.contractService.downloadPdf(contractId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `contract-${contractId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Failed to download PDF'),
    });
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

  canPartyAct(c: Contract): boolean {
    const s = String(c.status ?? '')
      .trim()
      .toUpperCase();
    return s === 'DRAFT' || s === 'ACTIVE';
  }

  onListCancel(c: Contract): void {
    if (this.cancelingId != null) return;
    if (!this.canPartyAct(c)) {
      this.toast.error('Only draft or active contracts can be cancelled.');
      return;
    }
    if (!confirm(`Cancel this contract? The project will reopen for the client and your assignment ends.`)) return;
    this.cancelingId = c.id;
    this.candidatureService.cancelContractAsFreelancer(c.id, this.freelancerId).subscribe({
      next: () => {
        this.contractService.getById(c.id).subscribe({
          next: (updated) => {
            this.cancelingId = null;
            const idx = this.contracts.findIndex((x) => x.id === c.id);
            if (idx !== -1) this.contracts[idx] = normalizeContractFromApi(updated);
            this.toast.success('Contract cancelled. Project reopened for the client.');
          },
          error: () => {
            this.cancelingId = null;
            this.load();
            this.toast.success('Contract cancelled');
          },
        });
      },
      error: (err: unknown) => {
        this.cancelingId = null;
        const m =
          err && typeof err === 'object' && err !== null && 'error' in err
            ? (err as { error?: { message?: string } }).error?.message
            : undefined;
        this.toast.error(m || 'Failed to cancel contract');
      },
    });
  }
}
