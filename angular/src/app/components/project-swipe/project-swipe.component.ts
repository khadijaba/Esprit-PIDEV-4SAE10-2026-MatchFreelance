import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../models/project.model';
import { ProjectSwipeCardComponent } from '../project-swipe-card/project-swipe-card.component';

@Component({
  selector: 'app-project-swipe',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ProjectSwipeCardComponent],
  templateUrl: './project-swipe.component.html',
})
export class ProjectSwipeComponent implements OnInit {
  stack: Project[] = [];
  loading = true;
  auth = inject(AuthService);
  get freelancerId(): number {
    return this.auth.currentUserId() ?? 2;
  }
  selectedProject: Project | null = null;
  applyMessage = '';
  proposedBudget = 0;
  extraTasksBudget = 0;
  applying = false;

  constructor(
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private toast: ToastService
  ) {}

  get topProject(): Project | undefined {
    return this.stack[0];
  }

  get backProjects(): Project[] {
    return this.stack.slice(1, 3);
  }

  ngOnInit() {
    forkJoin({
      projects: this.projectService.getByStatus('OPEN'),
      candidatures: this.candidatureService.getByFreelancerId(this.freelancerId),
    }).subscribe({
      next: ({ projects, candidatures }) => {
        const appliedIds = new Set(candidatures.map((c) => c.projectId));
        this.stack = projects.filter((p) => !appliedIds.has(p.id));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load projects');
      },
    });
  }

  onSkip() {
    if (this.stack.length > 0) {
      this.stack = this.stack.slice(1);
    }
  }

  onApply(project: Project) {
    this.selectedProject = project;
    this.proposedBudget = project.minBudget;
    this.extraTasksBudget = 0;
    this.applyMessage = '';
  }

  closeModal() {
    this.selectedProject = null;
  }

  onSubmitApply() {
    if (!this.selectedProject) return;
    const minB = this.selectedProject.minBudget;
    const maxB = this.selectedProject.maxBudget;
    const budget = Number(this.proposedBudget);
    if (Number.isNaN(budget) || budget < minB || budget > maxB) {
      this.toast.error(`Proposed budget must be between ${minB} and ${maxB} TND`);
      return;
    }
    this.applying = true;
    const extra = Number(this.extraTasksBudget) || 0;
    this.candidatureService
      .create({
        projectId: this.selectedProject.id,
        freelancerId: this.freelancerId,
        message: this.applyMessage,
        proposedBudget: budget,
        extraTasksBudget: extra >= 0 ? extra : undefined,
      })
      .subscribe({
        next: () => {
          this.toast.success('Application submitted');
          this.selectedProject = null;
          this.applying = false;
          this.stack = this.stack.slice(1);
        },
        error: (err) => {
          this.applying = false;
          this.toast.error(err?.error?.message || 'Failed to apply');
        },
      });
  }
}
