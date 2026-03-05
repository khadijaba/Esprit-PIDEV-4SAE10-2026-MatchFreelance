import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService } from '../../services/contract.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';
import { Candidature } from '../../models/candidature.model';

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './client-dashboard.component.html',
})
export class ClientDashboardComponent implements OnInit {
  projects: Project[] = [];
  candidatureCounts: Record<number, number> = {};
  pendingCounts: Record<number, number> = {};
  loading = true;
  get clientId(): number {
    return this.auth.currentUser()?.id ?? 0;
  }

  constructor(
    private auth: AuthService,
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private contractService: ContractService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.projectService.getByClientId(this.clientId).subscribe({
      next: (projects) => {
        this.projects = projects;
        this.loadContractsForInProgressProjects();
        this.loadCandidatureCounts();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load your projects');
      },
    });
  }

  loadContractsForInProgressProjects() {
    const needsContracts = this.projects.filter(
      (p) => (p.status === 'IN_PROGRESS' || p.status === 'COMPLETED') && (!p.contracts || p.contracts.length === 0)
    );
    if (needsContracts.length === 0) return;
    forkJoin(needsContracts.map((p) => this.contractService.getByProjectId(p.id))).subscribe({
      next: (contractArrays) => {
        needsContracts.forEach((p, i) => {
          p.contracts = (contractArrays[i] || []).map((c) => ({
            id: c.id,
            projectId: c.projectId,
            freelancerId: c.freelancerId,
            clientId: c.clientId,
            terms: c.terms,
            proposedBudget: c.proposedBudget,
            applicationMessage: c.applicationMessage,
            status: c.status,
            startDate: c.startDate,
            endDate: c.endDate,
            createdAt: c.createdAt,
          }));
        });
      },
    });
  }

  loadCandidatureCounts() {
    this.projects.forEach((p) => {
      this.candidatureService.getByProjectId(p.id).subscribe({
        next: (cands) => {
          this.candidatureCounts = { ...this.candidatureCounts, [p.id]: cands.length };
          this.pendingCounts = {
            ...this.pendingCounts,
            [p.id]: cands.filter((c) => c.status === 'PENDING').length,
          };
        },
      });
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

  formatStatus(status: ProjectStatus): string {
    return status.replace('_', ' ');
  }
}
