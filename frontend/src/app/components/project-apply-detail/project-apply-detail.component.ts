import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-project-apply-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-apply-detail.component.html',
})
export class ProjectApplyDetailComponent implements OnInit {
  project: Project | null = null;
  loading = true;
  applyMessage = '';
  proposedBudget = 0;
  extraTasksBudget: number | null = null;
  applying = false;
  hasApplied = false;

  constructor(
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private toast: ToastService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  get freelancerId(): number | null {
    const user = this.auth.getStoredUser();
    if (!user || user.role !== 'FREELANCER') return null;
    return user.userId ?? null;
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.router.navigate(['/projets']);
      return;
    }
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        this.proposedBudget = Number.isFinite(p.budget) && p.budget > 0 ? p.budget : 1;
        this.loading = false;
        this.loadHasApplied(id);
      },
      error: () => this.router.navigate(['/projets']),
    });
  }

  private loadHasApplied(projectId: number): void {
    const fid = this.freelancerId;
    if (!fid) {
      this.hasApplied = false;
      return;
    }
    this.candidatureService.getByFreelancerId(fid).subscribe({
      next: (rows) => {
        this.hasApplied = (rows ?? []).some((c) => c.projectId === projectId);
      },
      error: () => {
        this.hasApplied = false;
      },
    });
  }

  onApply(): void {
    if (!this.project) return;
    const fid = this.freelancerId;
    if (!fid) {
      this.toast.error('Connectez-vous en freelancer pour postuler.');
      this.router.navigate(['/login']);
      return;
    }
    if (!this.proposedBudget || this.proposedBudget <= 0) {
      this.toast.error('Budget propose invalide.');
      return;
    }
    this.applying = true;
    this.candidatureService
      .create({
        projectId: this.project.id,
        freelancerId: fid,
        message: this.applyMessage?.trim() || undefined,
        proposedBudget: Number(this.proposedBudget),
        extraTasksBudget:
          this.extraTasksBudget != null && Number.isFinite(this.extraTasksBudget) && this.extraTasksBudget >= 0
            ? Number(this.extraTasksBudget)
            : undefined,
      })
      .subscribe({
        next: () => {
          this.toast.success('Candidature envoyee.');
          this.hasApplied = true;
          this.applyMessage = '';
          this.applying = false;
        },
        error: (err) => {
          this.applying = false;
          this.toast.error(err?.error?.message || 'Echec envoi candidature.');
        },
      });
  }
}
