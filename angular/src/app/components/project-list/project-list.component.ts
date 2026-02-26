import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';
import { PageResponse } from '../../models/page.model';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-list.component.html',
})
export class ProjectListComponent implements OnInit {
  page?: PageResponse<Project>;
  projects: Project[] = [];
  searchTerm = '';
  statusFilter: ProjectStatus | '' = '';
  loading = true;
  pageIndex = 0;
  pageSize = 10;

  constructor(
    private projectService: ProjectService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.projectService
      .getPage({
        q: this.searchTerm.trim() || undefined,
        status: this.statusFilter || undefined,
        page: this.pageIndex,
        size: this.pageSize,
        sort: 'createdAt,desc',
      })
      .subscribe({
      next: (page) => {
        this.page = page;
        this.projects = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load projects. Is project-service running? (See run guide: Eureka → Gateway → project-service → Angular)');
      },
    });
  }

  onFiltersChange() {
    this.pageIndex = 0;
    this.load();
  }

  prevPage() {
    if (!this.page || this.pageIndex <= 0) return;
    this.pageIndex -= 1;
    this.load();
  }

  nextPage() {
    if (!this.page) return;
    if (this.pageIndex + 1 >= this.page.totalPages) return;
    this.pageIndex += 1;
    this.load();
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

