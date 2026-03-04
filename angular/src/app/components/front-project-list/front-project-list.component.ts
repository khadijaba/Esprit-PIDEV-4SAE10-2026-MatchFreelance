import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { Project, ProjectStatus } from '../../models/project.model';

export type ProjectSort = 'date-desc' | 'date-asc' | 'title' | 'budget-desc' | 'budget-asc';

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
  sortBy: ProjectSort = 'date-desc';
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
    this.filteredProjects = this.sortProjects(result);
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
      default:
        return arr;
    }
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
