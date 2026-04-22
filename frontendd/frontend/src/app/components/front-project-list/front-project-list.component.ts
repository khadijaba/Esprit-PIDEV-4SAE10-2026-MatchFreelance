import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { SkillGapService } from '../../services/skill-gap.service';
import { SkillService } from '../../services/skill.service';
import { AuthService } from '../../services/auth.service';
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
  /** Pagination */
  currentPage = 1;
  pageSize = 9;
  /** Compatibilité (0–100) par projet pour le freelancer connecté */
  compatibilityByProjectId: Record<number, number> = {};

  constructor(
    private projectService: ProjectService,
    private skillGapService: SkillGapService,
    private skillService: SkillService,
    public auth: AuthService,
  ) {}

  get isFreelancer(): boolean {
    return this.auth.isFreelancer();
  }

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
    if (this.isFreelancer) this.loadCompatibilities();
  }

  /** Charge les compétences du freelancer et calcule la compatibilité pour chaque projet. */
  private loadCompatibilities(): void {
    const user = this.auth.getCurrentUser();
    const userId = user?.id != null ? Number(user.id) : null;
    if (userId == null) return;
    this.skillService.getByFreelancerId(userId).subscribe({
      next: (freelancerSkills) => {
        const map: Record<number, number> = {};
        for (const project of this.projects) {
          const result = this.skillGapService.analyze(project, freelancerSkills);
          map[project.id] = result.compatibility;
        }
        this.compatibilityByProjectId = map;
      },
    });
  }

  getCompatibility(projectId: number): number | null {
    const v = this.compatibilityByProjectId[projectId];
    return v !== undefined ? v : null;
  }

  compatibilityClass(compatibility: number): string {
    if (compatibility >= 70) return 'text-emerald-600';
    if (compatibility >= 40) return 'text-amber-600';
    return 'text-red-600';
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
    this.currentPage = 1;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredProjects.length / this.pageSize));
  }

  get paginatedProjects(): Project[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredProjects.slice(start, start + this.pageSize);
  }

  goToPage(page: number) {
    this.currentPage = Math.max(1, Math.min(page, this.totalPages));
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
