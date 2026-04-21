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
  myProjects: Project[] = [];
  loading = true;
  error: string | null = null;
  applyOpenProjectId: number | null = null;
  applyLoading = false;
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
    this.projectService.getByStatus('OPEN').subscribe({
      next: (list) => {
        this.openProjects = list ?? [];
        this.loading = false;
      },
      error: () => {
        this.error =
          'Impossible de charger les projets. Vérifiez la Gateway (8050) et le microservice Project (8084).';
        this.loading = false;
      },
    });
    const u = this.auth.getStoredUser();
    if (this.auth.isProjectOwner() && u?.userId) {
      this.projectService.getByOwnerId(u.userId).subscribe({
        next: (list) => (this.myProjects = list ?? []),
        error: () => {},
      });
    }
  }

  isFreelancer(): boolean {
    return this.auth.getStoredUser()?.role === 'FREELANCER';
  }

  openApplyForm(project: Project): void {
    this.applyOpenProjectId = project.id;
    this.applyMessage = '';
    this.applyExtraBudget = null;
    this.applyBudget = Number.isFinite(project.budget) && project.budget > 0 ? project.budget : null;
  }

  closeApplyForm(): void {
    this.applyOpenProjectId = null;
    this.applyMessage = '';
    this.applyBudget = null;
    this.applyExtraBudget = null;
    this.applyLoading = false;
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
    this.applyLoading = true;
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
        this.applyLoading = false;
        this.toast.success('Candidature envoyee avec succes.');
        this.closeApplyForm();
      },
      error: (err) => {
        this.applyLoading = false;
        this.toast.error(err?.error?.message || 'Echec de la candidature.');
      },
    });
  }
}
