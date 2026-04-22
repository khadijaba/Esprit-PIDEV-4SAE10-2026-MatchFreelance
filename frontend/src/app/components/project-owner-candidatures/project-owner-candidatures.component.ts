import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap, tap, timeout } from 'rxjs/operators';
import { ProjectService } from '../../services/project.service';
import { CandidatureService } from '../../services/candidature.service';
import { ContractService } from '../../services/contract.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { FreelancerFitDto } from '../../models/freelancer-fit.model';
import { Project } from '../../models/project.model';
import { Candidature, Interview, InterviewStatus } from '../../models/candidature.model';
import { Contract } from '../../models/contract.model';
import { httpErrorMessage } from '../../utils/http-error.util';

@Component({
  selector: 'app-project-owner-candidatures',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-owner-candidatures.component.html',
})
export class ProjectOwnerCandidaturesComponent implements OnInit {
  projectId!: number;
  project?: Project;
  clientId!: number;
  candidatures: Candidature[] = [];
  contracts: Contract[] = [];
  /**
   * Objet remplace a chaque chargement (pas Map) pour que la detection de changements Angular
   * voie les entretiens — sinon les boutons Accepter/Refuser restent absents malgre des donnees en memoire.
   */
  interviewsByCandidatureId: Record<number, Interview[]> = {};
  expandedId: number | null = null;

  /** Métriques durée / score réussite par freelancer (GET freelancer-fit). */
  fitByFreelancerId: Record<number, FreelancerFitDto> = {};

  scheduleAt = '';
  scheduleNotes = '';
  loading = true;
  savingInterview = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private candidatureService: CandidatureService,
    private contractService: ContractService,
    private auth: AuthService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    if (!user?.id) {
      this.toast.error('Connectez-vous en tant que project owner.');
      void this.router.navigate(['/login']);
      return;
    }
    this.clientId = Number(user.id);
    this.projectId = +this.route.snapshot.paramMap.get('id')!;
    if (!this.projectId) {
      void this.router.navigate(['/project-owner/projects']);
      return;
    }
    void this.auth.ensureFreshTokenIfNeeded();
    this.projectService.getById(this.projectId).subscribe({
      next: (p) => {
        if (Number(p.projectOwnerId) !== this.clientId) {
          this.toast.error('Vous n’êtes pas propriétaire de ce projet.');
          void this.router.navigate(['/project-owner/projects']);
          return;
        }
        this.project = p;
        this.reloadAll();
      },
      error: () => {
        this.toast.error('Projet introuvable.');
        void this.router.navigate(['/project-owner/projects']);
      },
    });
  }

  reloadAll(): void {
    this.loading = true;
    this.candidatureService
      .listByProject(this.projectId, this.clientId)
      .pipe(
        switchMap((list) => {
          this.candidatures = [...list];
          this.interviewsByCandidatureId = {};
          if (!list.length) {
            return of(undefined);
          }
          return forkJoin(
            list.map((c) =>
              this.candidatureService.getInterviews(c.id, this.clientId).pipe(
                timeout(25_000),
                catchError(() => of([] as Interview[])),
                map((interviews) => this.normalizeInterviewList(interviews))
              )
            )
          ).pipe(
            tap((results) => {
              const next: Record<number, Interview[]> = {};
              list.forEach((c, i) => {
                next[c.id] = results[i] ?? [];
              });
              this.interviewsByCandidatureId = next;
            })
          );
        }),
        switchMap(() =>
          this.contractService.getByProjectId(this.projectId).pipe(catchError(() => of([] as Contract[])))
        )
      )
      .subscribe({
        next: (contracts) => {
          this.contracts = contracts ?? [];
          this.loading = false;
          this.loadFreelancerFitMetrics();
          this.cdr.detectChanges();
        },
        error: (err: HttpErrorResponse) => {
          this.loading = false;
          this.toast.error(httpErrorMessage(err, 'Impossible de charger les candidatures.'));
          this.cdr.detectChanges();
        },
      });
  }

  private loadFreelancerFitMetrics(): void {
    const ids = [...new Set(this.candidatures.map((c) => Number(c.freelancerId)).filter((n) => Number.isFinite(n)))];
    if (!ids.length) {
      this.fitByFreelancerId = {};
      return;
    }
    this.projectService.getFreelancerFit(this.projectId, ids).subscribe({
      next: (batch) => {
        const map: Record<number, FreelancerFitDto> = {};
        for (const row of batch.freelancers ?? []) {
          map[row.freelancerId] = row;
        }
        this.fitByFreelancerId = map;
        this.cdr.detectChanges();
      },
      error: () => {
        this.fitByFreelancerId = {};
      },
    });
  }

  fitFor(c: Candidature): FreelancerFitDto | undefined {
    return this.fitByFreelancerId[c.freelancerId];
  }

  toggleExpand(c: Candidature): void {
    this.expandedId = this.expandedId === c.id ? null : c.id;
    this.scheduleAt = '';
    this.scheduleNotes = '';
    if (this.expandedId != null) {
      const id = this.expandedId;
      this.candidatureService.getInterviews(id, this.clientId).subscribe({
        next: (list) => {
          this.interviewsByCandidatureId = {
            ...this.interviewsByCandidatureId,
            [id]: this.normalizeInterviewList(list),
          };
          this.cdr.detectChanges();
        },
        error: () => {
          this.interviewsByCandidatureId = { ...this.interviewsByCandidatureId, [id]: [] };
          this.cdr.detectChanges();
        },
      });
    }
  }

  interviewsFor(candidatureId: number): Interview[] {
    return this.interviewsByCandidatureId[candidatureId] ?? [];
  }

  /** Statut HTTP / JSON parfois en casse mixte ; necessaire pour boutons et « Marquer termine ». */
  private interviewStatusNorm(iv: Pick<Interview, 'status'>): string {
    return String(iv?.status ?? '')
      .trim()
      .toUpperCase();
  }

  isInterviewScheduled(iv: Interview): boolean {
    return this.interviewStatusNorm(iv) === 'SCHEDULED';
  }

  private normalizeInterviewList(raw: unknown): Interview[] {
    if (!Array.isArray(raw)) {
      return [];
    }
    return raw.map((item: Record<string, unknown>) => {
      const at = item['scheduledAt'] ?? item['scheduled_at'];
      let scheduledAt = '';
      if (at != null) {
        scheduledAt =
          typeof at === 'number'
            ? new Date(at).toISOString()
            : typeof at === 'string'
              ? at
              : String(at);
      }
      return {
        id: Number(item['id']),
        candidatureId: Number(item['candidatureId'] ?? item['candidature_id']),
        scheduledAt,
        status: String(item['status'] ?? 'SCHEDULED').trim().toUpperCase() as InterviewStatus,
        notes: (item['notes'] as string | null | undefined) ?? null,
      };
    });
  }

  isPending(c: Candidature): boolean {
    return String(c.status ?? '').toUpperCase() === 'PENDING';
  }

  /** Compteur affiche : priorite aux entretiens charges (API Interview), sinon metriques Candidature. */
  interviewCountDisplay(c: Candidature): number {
    const loaded = this.interviewsFor(c.id);
    if (loaded.length > 0) {
      return loaded.length;
    }
    return c.interviewCount ?? 0;
  }

  /**
   * Decision possible : candidature PENDING et tous les entretiens charges sont COMPLETED.
   * Si la liste n'est pas encore chargee (Feign OK), on s'appuie sur eligibleForAcceptance.
   */
  canDecide(c: Candidature): boolean {
    if (!this.isPending(c)) {
      return false;
    }
    const loaded = this.interviewsFor(c.id);
    if (loaded.length > 0) {
      return loaded.every((iv) => this.interviewStatusNorm(iv) === 'COMPLETED');
    }
    return c.eligibleForAcceptance === true;
  }

  /**
   * Uniquement un contrat en mission en cours masque Accepter/Refuser.
   * Les contrats CANCELLED / COMPLETED / DRAFT historiques ne bloquent plus la decision.
   */
  contractForProject(): Contract | undefined {
    const pid = Number(this.projectId);
    return this.contracts.find(
      (ct) => Number(ct.projectId) === pid && String(ct.status ?? '').toUpperCase() === 'ACTIVE'
    );
  }

  async submitSchedule(candidatureId: number): Promise<void> {
    if (!this.scheduleAt) {
      this.toast.error('Choisissez une date et heure pour l’entretien.');
      return;
    }
    const iso = new Date(this.scheduleAt).toISOString();
    this.savingInterview = true;
    await this.auth.ensureFreshTokenIfNeeded();
    this.candidatureService
      .scheduleInterview(candidatureId, this.clientId, {
        scheduledAt: iso,
        notes: this.scheduleNotes || undefined,
        status: 'SCHEDULED',
      })
      .subscribe({
        next: () => {
          this.savingInterview = false;
          this.scheduleAt = '';
          this.scheduleNotes = '';
          this.toast.success('Entretien planifié.');
          this.reloadAll();
          if (this.expandedId === candidatureId) {
            this.candidatureService.getInterviews(candidatureId, this.clientId).subscribe({
              next: (list) => {
                this.interviewsByCandidatureId = {
                  ...this.interviewsByCandidatureId,
                  [candidatureId]: this.normalizeInterviewList(list),
                };
                this.cdr.detectChanges();
              },
            });
          }
        },
        error: (err: HttpErrorResponse) => {
          this.savingInterview = false;
          this.toast.error(httpErrorMessage(err, 'Échec de la planification.'));
        },
      });
  }

  markInterviewCompleted(candidatureId: number, interview: Interview): void {
    const iso = new Date(interview.scheduledAt).toISOString();
    void this.auth.ensureFreshTokenIfNeeded();
    this.candidatureService
      .updateInterview(candidatureId, interview.id, this.clientId, {
        scheduledAt: iso,
        status: 'COMPLETED' as InterviewStatus,
        notes: interview.notes ?? undefined,
      })
      .subscribe({
        next: () => {
          this.toast.success('Entretien marqué comme terminé.');
          this.reloadAll();
          this.candidatureService.getInterviews(candidatureId, this.clientId).subscribe({
            next: (list) => {
              this.interviewsByCandidatureId = {
                ...this.interviewsByCandidatureId,
                [candidatureId]: this.normalizeInterviewList(list),
              };
              this.cdr.detectChanges();
            },
          });
        },
        error: (err: HttpErrorResponse) =>
          this.toast.error(httpErrorMessage(err, 'Mise à jour impossible.')),
      });
  }

  accept(c: Candidature): void {
    void this.auth.ensureFreshTokenIfNeeded();
    this.candidatureService.accept(c.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Candidature acceptée — contrat créé, projet en cours.');
        this.reloadAll();
      },
      error: (err: HttpErrorResponse) =>
        this.toast.error(httpErrorMessage(err, 'Acceptation impossible.')),
    });
  }

  reject(c: Candidature): void {
    void this.auth.ensureFreshTokenIfNeeded();
    this.candidatureService.reject(c.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Candidature refusée.');
        this.reloadAll();
      },
      error: (err: HttpErrorResponse) =>
        this.toast.error(httpErrorMessage(err, 'Refus impossible.')),
    });
  }

  payContract(): void {
    const ct = this.contractForProject();
    if (!ct) return;
    void this.auth.ensureFreshTokenIfNeeded();
    this.candidatureService.payContract(ct.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Paiement enregistré — mission terminée.');
        this.reloadAll();
      },
      error: (err: HttpErrorResponse) =>
        this.toast.error(httpErrorMessage(err, 'Paiement impossible.')),
    });
  }

  cancelContractFlow(): void {
    const ct = this.contractForProject();
    if (!ct) return;
    void this.auth.ensureFreshTokenIfNeeded();
    this.candidatureService.cancelContract(ct.id, this.clientId).subscribe({
      next: () => {
        this.toast.success('Contrat annulé — projet rouvert.');
        this.reloadAll();
      },
      error: (err: HttpErrorResponse) =>
        this.toast.error(httpErrorMessage(err, 'Annulation impossible.')),
    });
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bg-amber-100 text-amber-800',
      ACCEPTED: 'bg-emerald-100 text-emerald-800',
      REJECTED: 'bg-red-100 text-red-800',
    };
    return map[status] ?? 'bg-slate-100 text-slate-700';
  }

}
