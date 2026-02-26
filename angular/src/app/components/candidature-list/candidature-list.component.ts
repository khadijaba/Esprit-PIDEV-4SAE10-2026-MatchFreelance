import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CandidatureService } from '../../services/candidature.service';
import { ToastService } from '../../services/toast.service';
import { Candidature, CandidatureStatus } from '../../models/candidature.model';
import { PageResponse } from '../../models/page.model';

@Component({
  selector: 'app-candidature-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './candidature-list.component.html',
})
export class CandidatureListComponent implements OnInit {
  page?: PageResponse<Candidature>;
  candidatures: Candidature[] = [];
  loading = true;
  statusFilter: CandidatureStatus | '' = '';
  projectIdFilter: number | null = null;
  freelancerIdFilter: number | null = null;
  pageIndex = 0;
  pageSize = 10;

  constructor(
    private candidatureService: CandidatureService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.candidatureService
      .getPage({
        projectId: this.projectIdFilter ?? undefined,
        freelancerId: this.freelancerIdFilter ?? undefined,
        status: this.statusFilter || undefined,
        page: this.pageIndex,
        size: this.pageSize,
        sort: 'createdAt,desc',
      })
      .subscribe({
      next: (page) => {
        this.page = page;
        this.candidatures = page.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load candidatures');
      },
    });
  }

  onFiltersChange() {
    this.pageIndex = 0;
    this.load();
  }

  prevPage() {
    if (!this.page || this.pageIndex <= 0) return;
    this.pageIndex -= 1;
    this.load();
  }

  nextPage() {
    if (!this.page) return;
    if (this.pageIndex + 1 >= this.page.totalPages) return;
    this.pageIndex += 1;
    this.load();
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
}
