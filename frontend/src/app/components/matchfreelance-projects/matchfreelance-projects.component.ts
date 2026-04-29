import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../models/project.model';
import { CandidatureService } from '../../services/candidature.service';
import { CandidatureRequest } from '../../models/candidature.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-matchfreelance-projects',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './matchfreelance-projects.component.html',
})
export class MatchfreelanceProjectsComponent implements OnInit {
  openProjects: Project[] = [];
  allProjects: Project[] = [];
  myProjects: Project[] = [];
  loading = true;
  error: string | null = null;
  applyOpenProjectId: number | null = null;
  applyLoadingProjectId: number | null = null;
  applyMessage = '';
  applyBudget: number | null = null;
  applyExtraBudget: number | null = null;

  constructor(
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private toast: ToastService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProjects();
    const u = this.auth.getStoredUser();
    if (this.auth.isProjectOwner() && u?.userId) {
      this.projectService.getByOwnerId(u.userId).subscribe({
        next: (list) => (this.myProjects = list ?? []),
        error: () => {},
      });
    }
  }

  private loadProjects(): void {
    this.projectService.getAll().subscribe({
      next: (list) => {
        this.allProjects = list ?? [];
        this.openProjects = this.allProjects.filter((p) => p.status === 'OPEN');
        this.loading = false;
      },
      error: () => {
        this.error =
          'Impossible de charger les projets. Vérifiez la Gateway (8050) et le microservice Project (8084).';
        this.loading = false;
      },
    });
  }

  canApply(project: Project): boolean {
    return project.status === 'OPEN';
  }

  isFreelancer(): boolean {
    return this.auth.getStoredUser()?.role === 'FREELANCER';
  }

  openApplyForm(project: Project): void {
    this.applyOpenProjectId = project.id;
    this.applyMessage = '';
    this.applyExtraBudget = null;
    const [min, max] = this.getBudgetRange(project);
    this.applyBudget = min;
  }

  closeApplyForm(): void {
    this.applyOpenProjectId = null;
    this.applyMessage = '';
    this.applyBudget = null;
    this.applyExtraBudget = null;
    this.applyLoadingProjectId = null;
  }

  isApplying(projectId: number): boolean {
    return this.applyLoadingProjectId === projectId;
  }

  private extractErrorMessage(err: unknown): string {
    const e = err as { error?: unknown; message?: string } | null;
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) return body;
    if (body && typeof body === 'object') {
      const message = (body as { message?: unknown }).message;
      if (typeof message === 'string' && message.trim()) return message;
      const error = (body as { error?: unknown }).error;
      if (typeof error === 'string' && error.trim()) return error;
    }
    if (typeof e?.message === 'string' && e.message.trim()) return e.message;
    return 'Echec de la candidature.';
  }

  getBudgetRange(project: Project): [number, number] {
    const p = project as unknown as { minBudget?: number | null; maxBudget?: number | null; budget?: number | null };
    const minRaw = Number(p.minBudget);
    const maxRaw = Number(p.maxBudget);
    const fallback = Number(p.budget);
    const min = Number.isFinite(minRaw) && minRaw > 0 ? minRaw : Number.isFinite(fallback) && fallback > 0 ? fallback : 1;
    const max = Number.isFinite(maxRaw) && maxRaw > 0 ? maxRaw : Number.isFinite(fallback) && fallback > 0 ? fallback : min;
    return [Math.min(min, max), Math.max(min, max)];
  }

  submitApplication(project: Project): void {
    const user = this.auth.getStoredUser();
    if (!user?.userId || user.role !== 'FREELANCER') {
      this.toast.error('Connectez-vous en freelancer pour postuler.');
      return;
    }
    if (this.applyBudget == null || !Number.isFinite(this.applyBudget) || this.applyBudget <= 0) {
      this.toast.error('Budget propose invalide.');
      return;
    }
    const [minBudget, maxBudget] = this.getBudgetRange(project);
    if (this.applyBudget < minBudget || this.applyBudget > maxBudget) {
      this.toast.error(`Le budget propose doit etre entre ${minBudget} et ${maxBudget}.`);
      return;
    }
    this.applyLoadingProjectId = project.id;
    const req: CandidatureRequest = {
      projectId: project.id,
      freelancerId: user.userId,
      message: this.applyMessage?.trim() || undefined,
      proposedBudget: this.applyBudget,
      extraTasksBudget:
        this.applyExtraBudget != null && Number.isFinite(this.applyExtraBudget) && this.applyExtraBudget >= 0
          ? this.applyExtraBudget
          : undefined,
    };
    this.candidatureService.create(req).subscribe({
      next: () => {
        this.applyLoadingProjectId = null;
        this.toast.success('Candidature envoyee avec succes.');
        this.closeApplyForm();
      },
      error: (err) => {
        this.applyLoadingProjectId = null;
        this.toast.error(this.extractErrorMessage(err));
      },
    });
  }
}
