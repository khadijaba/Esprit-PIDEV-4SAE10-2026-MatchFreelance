import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CandidatureService } from '../../services/candidature.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Candidature, CandidatureStatus } from '../../models/candidature.model';
import { Project } from '../../models/project.model';

@Component({
  selector: 'app-freelancer-applications',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './freelancer-applications.component.html',
})
export class FreelancerApplicationsComponent implements OnInit {
  candidatures: Candidature[] = [];
  projectTitles: Record<number, string> = {};
  loading = true;
  editingId: number | null = null;
  editMessage = '';
  saving = false;

  private readonly auth = inject(AuthService);

  get freelancerId(): number {
    const id = this.auth.currentUserId();
    return id != null ? Number(id) : 2;
  }

  constructor(
    private candidatureService: CandidatureService,
    private projectService: ProjectService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.candidatureService.getByFreelancerId(this.freelancerId).subscribe({
      next: (data) => {
        this.candidatures = data;
        const projectIds = [...new Set(data.map((c) => c.projectId))];
        if (projectIds.length === 0) {
          this.loading = false;
          return;
        }
        forkJoin(
          projectIds.map((id) =>
            this.projectService.getById(id).pipe(catchError(() => of(null as Project | null)))
          )
        ).subscribe({
          next: (projects) => {
            projects.forEach((p, i) => {
              if (p?.title) this.projectTitles[projectIds[i]] = p.title;
            });
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          },
        });
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load applications');
      },
    });
  }

  canEdit(c: Candidature): boolean {
    return c.status === 'PENDING';
  }

  startEdit(c: Candidature) {
    this.editingId = c.id;
    this.editMessage = c.message || '';
  }

  cancelEdit() {
    this.editingId = null;
    this.editMessage = '';
  }

  saveEdit(c: Candidature) {
    if (!this.editMessage.trim()) {
      this.toast.error('Message cannot be empty');
      return;
    }
    this.saving = true;
    this.candidatureService
      .update(c.id, {
        projectId: c.projectId,
        freelancerId: c.freelancerId,
        message: this.editMessage,
        proposedBudget: c.proposedBudget,
      })
      .subscribe({
        next: () => {
          this.toast.success('Application updated');
          this.saving = false;
          this.editingId = null;
          this.load();
        },
        error: () => {
          this.saving = false;
          this.toast.error('Failed to update application');
        },
      });
  }

  revoke(c: Candidature) {
    if (!confirm('Revoke this application? This action cannot be undone.')) return;
    this.candidatureService.delete(c.id).subscribe({
      next: () => {
        this.toast.success('Application revoked');
        this.load();
      },
      error: () => {
        this.toast.error('Failed to revoke application');
      },
    });
  }

  statusClass(status: CandidatureStatus): string {
    const map: Record<CandidatureStatus, string> = {
      PENDING: 'bg-amber-100 text-amber-700',
      ACCEPTED: 'bg-emerald-100 text-emerald-700',
      REJECTED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  formatStatus(status: CandidatureStatus): string {
    return status.replace('_', ' ');
  }
}
