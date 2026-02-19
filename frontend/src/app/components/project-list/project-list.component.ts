import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-list.component.html',
})
export class ProjectListComponent implements OnInit {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  searchTerm = '';
  statusFilter: ProjectStatus | '' = '';
  loading = true;

  constructor(
    private projectService: ProjectService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.projectService.getAll().subscribe({
      next: (data) => {
        this.projects = data;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load projects');
      },
    });
  }

  applyFilters() {
    let result = this.projects;
    if (this.statusFilter) {
      result = result.filter((p) => p.status === this.statusFilter);
    }
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      result = result.filter((p) => p.title.toLowerCase().includes(q));
    }
    this.filteredProjects = result;
  }

  onDelete(id: number) {
    if (!confirm('Are you sure you want to delete this project?')) return;
    this.projectService.delete(id).subscribe({
      next: () => {
        this.toast.success('Project deleted');
        this.load();
      },
      error: () => this.toast.error('Failed to delete project'),
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

