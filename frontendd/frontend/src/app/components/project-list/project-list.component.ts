import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { ConfirmService } from '../../services/confirm.service';
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
    private toast: ToastService,
    private auth: AuthService,
    private router: Router,
    private confirmService: ConfirmService
  ) {}

  ngOnInit() {
    const url = window.location.pathname;
    if (url.includes('/project-owner/projects')) {
      const user = this.auth.getCurrentUser();
      const projectOwnerId = user?.id != null ? Number(user.id) : 0;
      if (!projectOwnerId) {
        this.toast.error('Session invalide. Veuillez vous reconnecter.');
        this.router.navigate(['/login']);
        return;
      }
      this.loading = true;
      this.projectService.getByOwnerId(projectOwnerId).subscribe({
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
    } else {
      this.load();
    }
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

  async onDelete(id: number) {
    const ok = await this.confirmService.confirm('Supprimer ce projet ?');
    if (!ok) return;
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

  isProjectOwnerRoute(): boolean {
    return window.location.pathname.includes('/project-owner');
  }

  /** Lien détail : Project Owner voit la page projet avec Top 5 (/projects/:id) ; Admin voit /admin/projects/:id */
  getProjectRoute(id: number): string {
    return this.isProjectOwnerRoute() ? `/projects/${id}` : `/admin/projects/${id}`;
  }

  getEditRoute(id: number): string {
    return this.isProjectOwnerRoute() ? `/project-owner/projects/${id}/edit` : `/admin/projects/${id}/edit`;
  }
}

