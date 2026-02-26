import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { InterviewService } from '../../services/interview.service';
import { ToastService } from '../../services/toast.service';
import { Interview, InterviewStatus, MeetingMode, PageResponse } from '../../models/interview.model';

export type InterviewListRole = 'admin' | 'client' | 'freelancer';

const CLIENT_ID = 1;
const FREELANCER_ID = 1;

@Component({
  selector: 'app-interview-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './interview-list.component.html',
})
export class InterviewListComponent implements OnInit {
  role: InterviewListRole = 'admin';
  page?: PageResponse<Interview>;
  interviews: Interview[] = [];
  loading = true;
  pageIndex = 0;
  pageSize = 10;

  statusFilter: InterviewStatus | '' = '';
  modeFilter: MeetingMode | '' = '';
  projectIdFilter: number | null = null;
  freelancerIdFilter: number | null = null;
  ownerIdFilter: number | null = null;
  fromDate = '';
  toDate = '';

  statusOptions: InterviewStatus[] = ['PROPOSED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
  modeOptions: MeetingMode[] = ['ONLINE', 'FACE_TO_FACE'];

  constructor(
    private route: ActivatedRoute,
    private interviewService: InterviewService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.route.data.subscribe((data) => {
      this.role = (data['role'] as InterviewListRole) ?? 'admin';
      this.applyRoleDefaults();
      this.load();
    });
  }

  private applyRoleDefaults() {
    if (this.role === 'client') {
      this.ownerIdFilter = CLIENT_ID;
    } else if (this.role === 'freelancer') {
      this.freelancerIdFilter = FREELANCER_ID;
    }
  }

  get isAdmin(): boolean {
    return this.role === 'admin';
  }

  get listTitle(): string {
    if (this.role === 'admin') return 'All interviews';
    if (this.role === 'client') return 'My project interviews';
    return 'My interviews';
  }

  load() {
    this.loading = true;
    const params: Record<string, unknown> = {
      page: this.pageIndex,
      size: this.pageSize,
      sort: 'startAt,desc',
    };
    if (this.statusFilter) params['status'] = this.statusFilter;
    if (this.modeFilter) params['mode'] = this.modeFilter;
    if (this.projectIdFilter != null) params['projectId'] = this.projectIdFilter;
    if (this.freelancerIdFilter != null) params['freelancerId'] = this.freelancerIdFilter;
    if (this.ownerIdFilter != null) params['ownerId'] = this.ownerIdFilter;
    if (this.fromDate) params['from'] = new Date(this.fromDate).toISOString();
    if (this.toDate) params['to'] = new Date(this.toDate).toISOString();

    this.interviewService.searchInterviews(params).subscribe({
      next: (p) => {
        this.page = p;
        this.interviews = p.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load interviews');
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

  confirm(i: Interview) {
    this.interviewService.confirmInterview(i.id).subscribe({
      next: () => {
        this.toast.success('Interview confirmed');
        this.load();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to confirm'),
    });
  }

  cancel(i: Interview) {
    this.interviewService.cancelInterview(i.id).subscribe({
      next: () => {
        this.toast.success('Interview cancelled');
        this.load();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to cancel'),
    });
  }

  complete(i: Interview) {
    this.interviewService.completeInterview(i.id).subscribe({
      next: () => {
        this.toast.success('Interview completed');
        this.load();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to complete'),
    });
  }

  statusClass(status: InterviewStatus): string {
    const map: Record<InterviewStatus, string> = {
      PROPOSED: 'bg-amber-100 text-amber-700',
      CONFIRMED: 'bg-sky-100 text-sky-700',
      COMPLETED: 'bg-emerald-100 text-emerald-700',
      CANCELLED: 'bg-red-100 text-red-700',
      NO_SHOW: 'bg-slate-100 text-slate-600',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  modeLabel(mode: MeetingMode): string {
    return mode === 'FACE_TO_FACE' ? 'Face to face' : 'Online';
  }

  mapUrl(i: Interview): string | null {
    if (i.mode !== 'FACE_TO_FACE') return null;
    if (i.lat != null && i.lng != null) {
      return `https://www.google.com/maps?q=${encodeURIComponent(`${i.lat},${i.lng}`)}`;
    }
    const q = [i.addressLine, i.city].filter(Boolean).join(', ');
    if (!q) return null;
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`;
  }
}
