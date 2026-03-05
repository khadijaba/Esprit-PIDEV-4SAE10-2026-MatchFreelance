import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { InterviewService } from '../../services/interview.service';
import { ToastService } from '../../services/toast.service';
import { Interview, InterviewStatus, MeetingMode, PageResponse } from '../../models/interview.model';

export type InterviewListRole = 'admin' | 'client' | 'freelancer';

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
  pageSize = 5;

  statusFilter: InterviewStatus | '' = '';
  modeFilter: MeetingMode | '' = '';
  projectIdFilter: number | null = null;
  freelancerIdFilter: number | null = null;
  ownerIdFilter: number | null = null;
  fromDate = '';
  toDate = '';

  statusOptions: InterviewStatus[] = ['PROPOSED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
  modeOptions: MeetingMode[] = ['ONLINE', 'FACE_TO_FACE'];

  /** freelancerId -> score 0-100 (client view) */
  reliabilityByFreelancerId: Record<number, number> = {};
  /** ownerId -> score 0-100 (freelancer view) */
  reliabilityByOwnerId: Record<number, number> = {};
  workloadLabel?: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private interviewService: InterviewService,
    private toast: ToastService,
    private auth: AuthService
  ) {}

  ngOnInit() {
    this.route.data.subscribe((data) => {
      this.role = (data['role'] as InterviewListRole) ?? 'admin';
      this.applyRoleDefaults();
      this.load();
    });
  }

  private applyRoleDefaults() {
    const userId = this.auth.currentUser()?.id;
    if (this.role === 'client' && userId) {
      this.ownerIdFilter = userId;
    } else if (this.role === 'freelancer' && userId) {
      this.freelancerIdFilter = userId;
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
        this.loadReliability();
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load interviews');
      },
    });
  }

  private loadReliability() {
    this.reliabilityByFreelancerId = {};
    this.reliabilityByOwnerId = {};

    if (this.role === 'client') {
      const freelancerIds = Array.from(new Set(this.interviews.map((i) => i.freelancerId)));
      if (freelancerIds.length === 0) return;
      forkJoin(freelancerIds.map((id) => this.interviewService.getReliabilityForFreelancer(id))).subscribe({
        next: (results) => {
          freelancerIds.forEach((id, idx) => {
            this.reliabilityByFreelancerId[id] = Math.round((results[idx]?.score ?? 0) * 100);
          });
        },
      });
    } else if (this.role === 'freelancer') {
      const ownerIds = Array.from(new Set(this.interviews.map((i) => i.ownerId)));
      if (ownerIds.length > 0) {
        forkJoin(ownerIds.map((id) => this.interviewService.getReliabilityForOwner(id))).subscribe({
          next: (results) => {
            ownerIds.forEach((id, idx) => {
              this.reliabilityByOwnerId[id] = Math.round((results[idx]?.score ?? 0) * 100);
            });
          },
        });
      }
      const flId = this.auth.currentUser()?.id;
      if (flId) this.interviewService.getWorkloadForFreelancer(flId).subscribe({
        next: (w) => {
          const hours = (w.totalMinutes7 / 60).toFixed(1);
          this.workloadLabel = `${w.level.toLowerCase()} (${w.interviewsNext7d} interviews, ${hours}h next 7 days)`;
        },
      });
    }
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

  reject(i: Interview) {
    this.interviewService.rejectInterview(i.id).subscribe({
      next: () => {
        this.toast.success('Interview rejected');
        this.load();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to reject'),
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

  noShow(i: Interview) {
    this.interviewService.noShowInterview(i.id).subscribe({
      next: () => {
        this.toast.success('Interview marked as no-show');
        this.load();
      },
      error: (err) => this.toast.error(err?.error?.message || 'Failed to mark as no-show'),
    });
  }

  /** True when the meeting end time has passed. Used to show Terminé / No show only after the slot. */
  isPastEnd(i: Interview): boolean {
    return new Date(i.endAt).getTime() <= Date.now();
  }

  canMarkNoShow(i: Interview): boolean {
    if (i.status !== 'CONFIRMED') return false;
    return this.isPastEnd(i);
  }

  /** Per-interview reliability label: for client = freelancer reliability, for freelancer = client (owner) reliability. */
  getReliabilityLabel(i: Interview): string {
    if (this.role === 'client') {
      const v = this.reliabilityByFreelancerId[i.freelancerId];
      return v != null ? `${v}%` : '—';
    }
    if (this.role === 'freelancer') {
      const v = this.reliabilityByOwnerId[i.ownerId];
      return v != null ? `${v}%` : '—';
    }
    return '—';
  }

  canJoinOnline(i: Interview): boolean {
    if (i.mode !== 'ONLINE') return false;
    const now = Date.now();
    const start = new Date(i.startAt).getTime();
    const end = new Date(i.endAt).getTime();
    return now >= start && now <= end;
  }

  timeLeftLabel(i: Interview): string {
    const now = Date.now();
    const start = new Date(i.startAt).getTime();
    const end = new Date(i.endAt).getTime();

    if (now < start) return `Starts in ${this.formatMs(start - now)}`;
    if (now <= end) return `In progress (${this.formatMs(end - now)} left)`;
    return 'Ended';
  }

  private formatMs(ms: number): string {
    const totalSeconds = Math.max(0, Math.floor(ms / 1000));
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);

    const parts: string[] = [];
    if (days) parts.push(`${days}d`);
    if (hours) parts.push(`${hours}h`);
    parts.push(`${minutes}m`);
    return parts.join(' ');
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

  /** For freelancer view: distance from their location to FACE_TO_FACE interview location (km). */
  distanceLabel(i: Interview): string {
    if (i.mode !== 'FACE_TO_FACE') return '—';
    const user = this.auth.currentUser();
    if (user?.lat == null || user?.lng == null) return '—';
    if (i.lat == null || i.lng == null) return '—';
    const km = this.haversineKm(user.lat!, user.lng!, i.lat, i.lng);
    return km < 1 ? `${Math.round(km * 1000)} m` : `${km.toFixed(1)} km`;
  }

  private haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371;
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLon = ((lon2 - lon1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
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

  /** Router link to interview detail (client/freelancer only). */
  detailLink(i: Interview): string[] | null {
    if (this.role === 'admin') return null;
    if (this.role === 'client') return ['/client/interviews', String(i.id)];
    if (this.role === 'freelancer') return ['/freelancer/interviews', String(i.id)];
    return null;
  }

  goToDetail(i: Interview) {
    const link = this.detailLink(i);
    if (link) this.router.navigate(link);
  }
}
