import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { Project, ProjectStatus } from '../../models/project.model';

@Component({
  selector: 'app-front-project-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './front-project-list.component.html',
})
export class FrontProjectListComponent implements OnInit {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  searchTerm = '';
  statusFilter: ProjectStatus | '' = '';
  loading = true;

  constructor(private projectService: ProjectService) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    if (this.statusFilter) {
      this.projectService.getByStatus(this.statusFilter).subscribe({
        next: (data) => this.handleLoaded(data),
        error: () => (this.loading = false),
      });
    } else {
      this.projectService.getAll().subscribe({
        next: (data) => this.handleLoaded(data),
        error: () => (this.loading = false),
      });
    }
  }

  private handleLoaded(data: Project[]) {
    this.projects = data;
    this.applyFilters();
    this.loading = false;
  }

  applyFilters() {
    let result = this.projects;
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      result = result.filter((p) =>
        p.title.toLowerCase().includes(q) || p.description.toLowerCase().includes(q)
      );
    }
    this.filteredProjects = result;
  }

  onStatusChange() {
    this.load();
  }

  statusClass(status: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      OPEN: 'bg-emerald-100 text-emerald-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-slate-100 text-slate-600',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-slate-100 text-slate-600';
  }

  formatStatus(status: ProjectStatus): string {
    return status.replace('_', ' ');
  }
}
