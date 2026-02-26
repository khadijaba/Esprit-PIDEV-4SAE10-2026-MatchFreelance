import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  projects: Project[] = [];
  stats = { total: 0, open: 0, inProgress: 0, completed: 0, cancelled: 0, totalBudget: 0 };
  loadError = false;

  constructor(
    private projectService: ProjectService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.projectService.getAll().subscribe({
      next: (data) => {
        this.projects = data;
        this.stats.total = data.length;
        this.stats.open = data.filter((p) => p.status === 'OPEN').length;
        this.stats.inProgress = data.filter((p) => p.status === 'IN_PROGRESS').length;
        this.stats.completed = data.filter((p) => p.status === 'COMPLETED').length;
        this.stats.cancelled = data.filter((p) => p.status === 'CANCELLED').length;
        this.stats.totalBudget = data.reduce((sum, p) => sum + (p.minBudget + p.maxBudget) / 2, 0);
      },
      error: () => {
        this.loadError = true;
        this.toast.error('Failed to load projects. Is project-service running?');
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

  formatStatus(status: ProjectStatus): string {
    return status.replace('_', ' ');
  }
}

