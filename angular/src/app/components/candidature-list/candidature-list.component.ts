import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CandidatureService } from '../../services/candidature.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { Candidature, CandidatureStatus } from '../../models/candidature.model';

@Component({
  selector: 'app-candidature-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './candidature-list.component.html',
})
export class CandidatureListComponent implements OnInit {
  candidatures: Candidature[] = [];
  projectTitles: Record<number, string> = {};
  loading = true;
  searchQuery = '';
  statusFilter: CandidatureStatus | '' = '';

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
    this.candidatureService.getAll().subscribe({
      next: (data) => {
        this.candidatures = data;
        const projectIds = [...new Set(data.map((c) => c.projectId))];
        if (projectIds.length === 0) {
          this.loading = false;
          return;
        }
        forkJoin(projectIds.map((id) => this.projectService.getById(id))).subscribe({
          next: (projects) => {
            projects.forEach((p, i) => (this.projectTitles[projectIds[i]] = p.title));
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          },
        });
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load candidatures');
      },
    });
  }

  onAccept(c: Candidature) {
    this.candidatureService.accept(c.id).subscribe({
      next: () => {
        this.toast.success('Candidature accepted');
        this.load();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to accept'),
    });
  }

  onReject(c: Candidature) {
    this.candidatureService.reject(c.id).subscribe({
      next: () => {
        this.toast.success('Candidature rejected');
        this.load();
      },
      error: () => this.toast.error('Failed to reject'),
    });
  }

  onDelete(id: number) {
    if (!confirm('Delete this candidature?')) return;
    this.candidatureService.delete(id).subscribe({
      next: () => {
        this.toast.success('Candidature deleted');
        this.load();
      },
      error: () => this.toast.error('Failed to delete'),
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

  get filteredCandidatures(): Candidature[] {
    let list = this.candidatures;
    const q = this.searchQuery.trim().toLowerCase();
    if (q) {
      list = list.filter((c) => {
        const projectTitle = (this.projectTitles[c.projectId] ?? '').toLowerCase();
        const freelancer = (c.freelancerName ?? `freelancer #${c.freelancerId}`).toLowerCase();
        const message = (c.message ?? '').toLowerCase();
        const id = String(c.id);
        return (
          projectTitle.includes(q) ||
          freelancer.includes(q) ||
          message.includes(q) ||
          id.includes(q)
        );
      });
    }
    if (this.statusFilter !== '') {
      list = list.filter((c) => c.status === this.statusFilter);
    }
    return list;
  }
}
