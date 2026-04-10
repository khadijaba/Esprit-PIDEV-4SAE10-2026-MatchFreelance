import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
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
  candidatureCounts: Record<number, number | undefined> = {};
  pendingCounts: Record<number, number | undefined> = {};
  loading = true;
  clientId = 1;

  constructor(
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private toast: ToastService
  ) { }

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.projectService.getByClientId(this.clientId).subscribe({
      next: (projects) => {
        this.projects = projects;
        this.loadCandidatureCounts();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load your projects');
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
