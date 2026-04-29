import { Component, Input, OnInit } from '@angular/core';

import { CommonModule } from '@angular/common';

import { FormsModule } from '@angular/forms';

import { RouterLink } from '@angular/router';

import { forkJoin, of } from 'rxjs';

import { catchError } from 'rxjs/operators';

import { CandidatureService } from '../../services/candidature.service';

import { ToastService } from '../../services/toast.service';

import { AuthService } from '../../services/auth.service';

import { ProjectService } from '../../services/project.service';

import { InterviewService } from '../../services/interview.service';

import { CandidatureRequest, CandidatureResponse, CandidatureStatus } from '../../models/candidature.model';

import { Interview } from '../../models/interview.model';



@Component({

  selector: 'app-freelancer-applications',

  standalone: true,

  imports: [CommonModule, FormsModule, RouterLink],

  templateUrl: './freelancer-applications.component.html',

})

export class FreelancerApplicationsComponent implements OnInit {

  /** Intégration dashboard : titre réduit + lien « Tout voir ». */

  @Input() embedMode = false;

  /** Limite d’items affichés en mode embed (null = illimité). */

  @Input() maxEmbedItems: number | null = null;

  /** Quand le dashboard fournit déjà le titre (évite le doublon). */
  @Input() hideEmbeddedHeader = false;

  /** Page complète : tableau (style FreelanceHub) ou cartes détaillées. */
  layout: 'table' | 'cards' = 'table';

  get tableColspan(): number {
    return this.embedMode ? 5 : 6;
  }

  candidatures: CandidatureResponse[] = [];

  projectTitles: Record<number, string> = {};

  loading = true;

  editingId: number | null = null;

  editMessage = '';

  saving = false;



  expandedCandidatureId: number | null = null;

  interviewsByCandidature: Record<number, Interview[]> = {};

  interviewsLoading: Record<number, boolean> = {};



  constructor(

    private candidatureService: CandidatureService,

    private projectService: ProjectService,

    private interviewService: InterviewService,

    private toast: ToastService,

    private auth: AuthService

  ) {}



  get freelancerId(): number | null {

    const u = this.auth.getStoredUser();

    return u?.role === 'FREELANCER' ? u.userId : null;

  }



  ngOnInit(): void {

    this.load();

  }

  setLayout(l: 'table' | 'cards'): void {
    this.layout = l;
    this.expandedCandidatureId = null;
    this.editingId = null;
  }



  load(): void {

    const fid = this.freelancerId;

    if (!fid) {

      this.loading = false;

      this.candidatures = [];

      return;

    }

    this.loading = true;

    this.candidatureService.getByFreelancerId(fid).subscribe({

      next: (data) => {

        let list = data ?? [];

        if (this.embedMode && this.maxEmbedItems != null && this.maxEmbedItems > 0) {

          list = list.slice(0, this.maxEmbedItems);

        }

        this.candidatures = list;

        this.loadProjectTitles(list);

        this.loading = false;

      },

      error: () => {

        this.loading = false;

        this.toast.error('Echec chargement candidatures.');

      },

    });

  }



  private loadProjectTitles(list: CandidatureResponse[]): void {

    const ids = [...new Set(list.map((c) => c.projectId).filter((id) => id != null))];

    if (ids.length === 0) return;

    forkJoin(

      ids.map((id) =>

        this.projectService.getById(id).pipe(

          catchError(() => of(null))

        )

      )

    ).subscribe((projects) => {

      const next: Record<number, string> = { ...this.projectTitles };

      projects.forEach((p, i) => {

        const pid = ids[i];

        if (p?.title) next[pid] = p.title;

      });

      this.projectTitles = next;

    });

  }



  projectLabel(c: CandidatureResponse): string {

    return this.projectTitles[c.projectId] ?? `Projet #${c.projectId}`;

  }



  appliedAtLabel(c: CandidatureResponse): string {

    if (!c.createdAt) return '—';

    const d = new Date(c.createdAt);

    if (Number.isNaN(d.getTime())) return '—';

    return new Intl.DateTimeFormat('fr-FR', {

      dateStyle: 'medium',

      timeStyle: 'short',

    }).format(d);

  }



  interviewCountLabel(c: CandidatureResponse): string {

    const n = c.interviewCount;

    return n != null ? String(n) : '—';

  }



  showPendingInterviewHint(c: CandidatureResponse): boolean {

    return (

      c.status === 'PENDING' &&

      (c.interviewCount ?? 0) > 0 &&

      c.eligibleForAcceptance !== true

    );

  }



  toggleInterviews(c: CandidatureResponse): void {

    const fid = this.freelancerId;

    if (!fid) return;

    if (this.expandedCandidatureId === c.id) {

      this.expandedCandidatureId = null;

      return;

    }

    this.expandedCandidatureId = c.id;

    if (this.interviewsByCandidature[c.id]?.length) {

      this.refreshMetrics(c.id, fid);

      return;

    }

    this.interviewsLoading[c.id] = true;

    this.interviewService.getForFreelancer(c.id, fid).subscribe({

      next: (list) => {

        this.interviewsByCandidature = { ...this.interviewsByCandidature, [c.id]: list ?? [] };

        this.interviewsLoading[c.id] = false;

        this.refreshMetrics(c.id, fid);

      },

      error: () => {

        this.interviewsLoading[c.id] = false;

        this.toast.error('Impossible de charger les entretiens.');

      },

    });

  }



  private refreshMetrics(candidatureId: number, fid: number): void {

    this.interviewService.getMetrics(candidatureId, fid).subscribe({

      next: (m) => {

        this.candidatures = this.candidatures.map((row) =>

          row.id === candidatureId

            ? {

                ...row,

                interviewCount: m.interviewCount ?? row.interviewCount,

                eligibleForAcceptance: m.eligibleForAcceptance ?? row.eligibleForAcceptance,

              }

            : row

        );

      },

      error: () => {},

    });

  }



  interviewsFor(c: CandidatureResponse): Interview[] {

    return this.interviewsByCandidature[c.id] ?? [];

  }



  interviewStatusClass(s: string): string {

    const map: Record<string, string> = {

      SCHEDULED: 'bg-sky-100 text-sky-800',

      COMPLETED: 'bg-emerald-100 text-emerald-800',

      CANCELLED: 'bg-slate-200 text-slate-700',

      NO_SHOW: 'bg-amber-100 text-amber-800',

    };

    return map[s] ?? 'bg-gray-100 text-gray-700';

  }



  formatInterviewWhen(iso: string): string {

    const d = new Date(iso);

    if (Number.isNaN(d.getTime())) return iso;

    return new Intl.DateTimeFormat('fr-FR', {

      dateStyle: 'medium',

      timeStyle: 'short',

    }).format(d);

  }



  canEdit(c: CandidatureResponse): boolean {

    return c.status === 'PENDING';

  }



  startEdit(c: CandidatureResponse): void {

    this.editingId = c.id;

    this.editMessage = c.message || '';

  }



  cancelEdit(): void {

    this.editingId = null;

    this.editMessage = '';

  }



  saveEdit(c: CandidatureResponse): void {

    if (!this.editMessage.trim()) {

      this.toast.error('Le message ne peut pas etre vide.');

      return;

    }

    const payload: CandidatureRequest = {

      projectId: c.projectId,

      freelancerId: c.freelancerId,

      message: this.editMessage.trim(),

      proposedBudget: c.proposedBudget,

      extraTasksBudget: c.extraTasksBudget ?? undefined,

    };

    this.saving = true;

    this.candidatureService.update(c.id, payload).subscribe({

      next: () => {

        this.saving = false;

        this.toast.success('Candidature mise a jour.');

        this.cancelEdit();

        this.load();

      },

      error: () => {

        this.saving = false;

        this.toast.error('Echec mise a jour.');

      },

    });

  }



  revoke(c: CandidatureResponse): void {

    if (!confirm('Retirer cette candidature ?')) return;

    this.candidatureService.delete(c.id).subscribe({

      next: () => {

        this.toast.success('Candidature retiree.');

        this.load();

      },

      error: () => this.toast.error('Echec retrait candidature.'),

    });

  }



  statusClass(status: CandidatureStatus): string {

    const map: Record<string, string> = {

      PENDING: 'bg-amber-100 text-amber-700',

      ACCEPTED: 'bg-emerald-100 text-emerald-700',

      REJECTED: 'bg-red-100 text-red-700',

      WITHDRAWN: 'bg-slate-100 text-slate-700',

    };

    return map[status] ?? 'bg-gray-100 text-gray-700';

  }

}

