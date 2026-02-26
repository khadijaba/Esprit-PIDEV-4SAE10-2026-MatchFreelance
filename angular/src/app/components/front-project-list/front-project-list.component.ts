import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';
import { PageResponse } from '../../models/page.model';

@Component({
  selector: 'app-front-project-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './front-project-list.component.html',
})
export class FrontProjectListComponent implements OnInit {
  page?: PageResponse<Project>;
  projects: Project[] = [];
  searchTerm = '';
  statusFilter: ProjectStatus | '' = '';
  loading = true;
  pageIndex = 0;
  pageSize = 9;

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
          this.toast.error('Could not load projects. Is project-service running?');
        },
      });
  }

  onStatusChange() {
    this.pageIndex = 0;
    this.load();
  }

  onSearchChange() {
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
