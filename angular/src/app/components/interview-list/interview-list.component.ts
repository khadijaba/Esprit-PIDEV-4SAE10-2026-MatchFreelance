import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { InterviewService } from '../../services/interview.service';
import { ToastService } from '../../services/toast.service';
import { UserService } from '../../services/user.service';
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
  /** Client view: filter by freelancer name. Freelancer view: filter by client name. (client-side) */
  nameFilter = '';
  /** Min reliability 0-100 (client/freelancer view). null = any. (client-side) */
  minFiabilityFilter: number | null = null;

  statusOptions: InterviewStatus[] = ['PROPOSED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
  modeOptions: MeetingMode[] = ['ONLINE', 'FACE_TO_FACE'];
  fiabilityOptions: { value: number | null; label: string }[] = [
    { value: null, label: 'Tous' },
    { value: 25, label: '25 % et plus' },
    { value: 50, label: '50 % et plus' },
    { value: 75, label: '75 % et plus' },
    { value: 100, label: '100 %' },
  ];

  /** freelancerId -> score 0-100 (client view) */
  reliabilityByFreelancerId: Record<number, number> = {};
  /** ownerId -> score 0-100 (freelancer view) */
  reliabilityByOwnerId: Record<number, number> = {};
  workloadLabel?: string;
  /** userId -> display name (for freelancer column in client view, client column in freelancer view) */
  displayNamesByUserId: Record<number, string> = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private interviewService: InterviewService,
    private userService: UserService,
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

  /** Interviews after client-side name + fiability filter (client/freelancer only). */
  get filteredInterviews(): Interview[] {
    let list = this.interviews;
    if (this.role !== 'client' && this.role !== 'freelancer') return list;
    const nameTrim = this.nameFilter.trim().toLowerCase();
    if (nameTrim) {
      list = list.filter((i) => {
        const id = this.role === 'client' ? i.freelancerId : i.ownerId;
        const name = this.displayNamesByUserId[id] ?? '';
        return name.toLowerCase().includes(nameTrim);
      });
    }
    if (this.minFiabilityFilter != null) {
      list = list.filter((i) => {
        const score =
          this.role === 'client'
            ? this.reliabilityByFreelancerId[i.freelancerId]
            : this.reliabilityByOwnerId[i.ownerId];
        return score != null && score >= this.minFiabilityFilter!;
      });
    }
    return list;
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
        this.loadDisplayNames();
        this.loadReliability();
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load interviews');
      },
    });
  }

  private loadDisplayNames() {
    this.displayNamesByUserId = {};
    const ids: number[] =
      this.role === 'client'
        ? Array.from(new Set(this.interviews.map((i) => i.freelancerId)))
        : this.role === 'freelancer'
          ? Array.from(new Set(this.interviews.map((i) => i.ownerId)))
          : [];
    if (ids.length === 0) return;
    this.userService.getDisplayNamesMap(ids).subscribe({
      next: (map) => (this.displayNamesByUserId = map),
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

  /** Tunisian state/city name -> [lat, lng] (approximate center) for distance between states. */
  private static readonly STATE_COORDS: Record<string, [number, number]> = {
    tunis: [36.8065, 10.1815],
    sfax: [34.7406, 10.7603],
    sousse: [35.8256, 10.6346],
    nabeul: [36.4561, 10.7376],
    bizerte: [37.2744, 9.8739],
    monastir: [35.777, 10.8261],
    gabes: [33.8815, 10.0982],
    kairouan: [35.6781, 10.0963],
    gafsa: [34.425, 8.7842],
    medenine: [33.3549, 10.5055],
    beja: [36.7256, 9.1817],
    kef: [36.1822, 8.7148],
    siliana: [36.0889, 9.3644],
    kasserine: [35.1673, 8.8361],
    sidi: [35.6673, 10.8907],
    mahdia: [35.5047, 11.0447],
    zaghouan: [36.4029, 10.1429],
    manouba: [36.8081, 10.0972],
    ariana: [36.8625, 10.1934],
    'ben arous': [36.7533, 10.2189],
    kebili: [33.7072, 8.9711],
    tatouine: [32.9297, 10.4511],
    'sidi bouzid': [35.0381, 9.4842],
    jendouba: [36.5011, 8.7803],
  };

  private static coordsFromCityOrState(text: string | null | undefined): [number, number] | null {
    if (!text || !text.trim()) return null;
    const lower = text.toLowerCase().trim();
    for (const [key, coords] of Object.entries(InterviewListComponent.STATE_COORDS)) {
      if (lower.includes(key)) return coords;
    }
    return null;
  }

  /** For freelancer view: distance from their state/city to FACE_TO_FACE interview state/city (km). */
  distanceLabel(i: Interview): string {
    if (i.mode !== 'FACE_TO_FACE') return '—';
    const user = this.auth.currentUser();
    const userCoords = InterviewListComponent.coordsFromCityOrState(user?.city);
    const interviewCoords = InterviewListComponent.coordsFromCityOrState(i.city) ?? InterviewListComponent.coordsFromCityOrState(i.addressLine);
    if (!userCoords || !interviewCoords) return '—';
    const km = this.haversineKm(userCoords[0], userCoords[1], interviewCoords[0], interviewCoords[1]);
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
