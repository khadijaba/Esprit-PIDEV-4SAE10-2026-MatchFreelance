import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ToastService } from '../../services/toast.service';
import { Project, ProjectStatus } from '../../models/project.model';
import { Candidature, CandidatureStatus } from '../../models/candidature.model';
import { InterviewScheduleComponent } from '../interview-schedule/interview-schedule.component';

@Component({
  selector: 'app-project-details',
  standalone: true,
  imports: [CommonModule, RouterLink, InterviewScheduleComponent],
  templateUrl: './project-details.component.html',
})
export class ProjectDetailsComponent implements OnInit {
  project?: Project;
  candidatures: Candidature[] = [];
  loading = true;

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
        this.loadCandidatures(id);
        this.loading = false;
      },
      error: () => {
        this.toast.error('Project not found');
        this.router.navigate(['/admin/projects']);
      },
    });
  }

  loadCandidatures(projectId: number) {
    this.candidatureService.getByProjectId(projectId).subscribe({
      next: (data) => (this.candidatures = data),
    });
  }

  onAccept(c: Candidature) {
    const clientId = this.project?.clientId ?? 1;
    this.candidatureService.accept(c.id, clientId).subscribe({
      next: () => {
        this.toast.success('Candidature accepted');
        if (this.project) this.loadCandidatures(this.project.id);
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to accept'),
    });
  }

  onReject(c: Candidature) {
    this.candidatureService.reject(c.id).subscribe({
      next: () => {
        this.toast.success('Candidature rejected');
        if (this.project) this.loadCandidatures(this.project.id);
      },
      error: () => this.toast.error('Failed to reject'),
    });
  }

  onDelete() {
    if (!this.project || !confirm('Delete this project?')) return;
    this.projectService.delete(this.project.id).subscribe({
      next: () => {
        this.toast.success('Project deleted');
        this.router.navigate(['/projects']);
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

  candidatureStatusClass(status: CandidatureStatus): string {
    const map: Record<CandidatureStatus, string> = {
      PENDING: 'bg-amber-100 text-amber-700',
      ACCEPTED: 'bg-emerald-100 text-emerald-700',
      REJECTED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }
}

