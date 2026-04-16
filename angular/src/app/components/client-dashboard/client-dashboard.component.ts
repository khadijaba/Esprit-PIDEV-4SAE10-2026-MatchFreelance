import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService } from '../../services/contract.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';
import { Candidature } from '../../models/candidature.model';

export type ClientProjectSort = 'title' | 'date-desc' | 'date-asc' | 'budget-desc' | 'budget-asc' | 'status';

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './client-dashboard.component.html',
})
export class ClientDashboardComponent implements OnInit {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  candidatureCounts: Record<number, number> = {};
  pendingCounts: Record<number, number> = {};
  loading = true;
  clientId = 1;
  searchTerm = '';
  sortBy: ClientProjectSort = 'date-desc';

  constructor(
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
        this.applyFilters();
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

  applyFilters() {
    let result = [...this.projects];
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      result = result.filter(
        (p) =>
          p.title.toLowerCase().includes(q) ||
          p.description.toLowerCase().includes(q)
      );
    }
    result = this.sortProjects(result);
    this.filteredProjects = result;
  }

  private sortProjects(list: Project[]): Project[] {
    const arr = [...list];
    switch (this.sortBy) {
      case 'title':
        return arr.sort((a, b) => a.title.localeCompare(b.title));
      case 'date-desc':
        return arr.sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
      case 'date-asc':
        return arr.sort(
          (a, b) =>
            new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );
      case 'budget-desc':
        return arr.sort((a, b) => b.maxBudget - a.maxBudget);
      case 'budget-asc':
        return arr.sort((a, b) => a.minBudget - b.minBudget);
      case 'status':
        const order: ProjectStatus[] = ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
        return arr.sort(
          (a, b) => order.indexOf(a.status) - order.indexOf(b.status)
        );
      default:
        return arr;
    }
  }

  onSearchOrSortChange() {
    this.applyFilters();
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
            extraTasksBudget: c.extraTasksBudget,
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
