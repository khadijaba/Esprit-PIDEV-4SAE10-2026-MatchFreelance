import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';

@Component({
  selector: 'app-front-project-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './front-project-detail.component.html',
})
export class FrontProjectDetailComponent implements OnInit {
  project?: Project;
  loading = true;
  applyMessage = '';
  proposedBudget = 0;
  freelancerId = 1;
  applying = false;

  constructor(
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        this.proposedBudget = p.minBudget;
        this.loading = false;
      },
      error: () => this.router.navigate(['/projects']),
    });
  }

  onApply() {
    if (!this.project) return;
    if (!this.proposedBudget || this.proposedBudget < this.project.minBudget || this.proposedBudget > this.project.maxBudget) {
      this.toast.error('Proposed budget must be between ' + this.project.minBudget + ' and ' + this.project.maxBudget);
      return;
    }
    this.applying = true;
    this.candidatureService.create({
      projectId: this.project.id,
      freelancerId: this.freelancerId,
      message: this.applyMessage,
      proposedBudget: this.proposedBudget,
    }).subscribe({
      next: () => {
        this.toast.success('Application submitted');
        this.applyMessage = '';
        this.proposedBudget = 0;
        this.applying = false;
      },
      error: (err) => {
        this.applying = false;
        this.toast.error(err?.error?.message || 'Failed to apply');
      },
    });
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
