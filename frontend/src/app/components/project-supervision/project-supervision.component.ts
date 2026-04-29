import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  EMPTY,
  Observable,
  catchError,
  concatMap,
  finalize,
  forkJoin,
  from,
  map,
  of,
  switchMap,
  tap,
  toArray,
} from 'rxjs';
import { ProjectService } from '../../services/project.service';
import { ProjectSupervisionService, CreatePhaseBody } from '../../services/project-supervision.service';
import { TeamAiService, AnalyzeProjectResponse } from '../../services/team-ai.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Project } from '../../models/project.model';
import { ProjectInsights } from '../../models/project-insights.model';
import { DecisionCopilotResponse, ProjectPhase } from '../../models/project-phase.model';
import { PhaseDeliverable } from '../../models/phase-deliverable.model';
import { ProjectMlRisk } from '../../models/project-ml-risk.model';

@Component({
  selector: 'app-project-supervision',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-supervision.component.html',
})
export class ProjectSupervisionComponent implements OnInit {
  projectId!: number;
  project: Project | null = null;
  insights: ProjectInsights | null = null;
  phases: ProjectPhase[] = [];
  selectedPhase: ProjectPhase | null = null;
  copilot: DecisionCopilotResponse | null = null;
  copilotLoading = false;
  /** Livrables de la phase sélectionnée (détail + actions). */
  deliverables: PhaseDeliverable[] = [];
  /** Livrables par phase (liste latérale). */
  deliverablesByPhaseId: Record<number, PhaseDeliverable[]> = {};
  deliverablesLoadingAll = false;
  savingDeliverable = false;
  reviewingId: number | null = null;
  newDeliverableTitle = '';
  newDeliverableDescription = '';
  newDeliverableType: 'DOC' | 'DESIGN' | 'CODE' | 'DEMO' | 'REPORT' = 'DOC';
  closingPhase = false;
  useLlmPlanning = false;

  phaseName = '';
  phaseOrder = 1;
  phaseDescription = '';
  phaseStart = '';
  phaseDue = '';
  savingPhase = false;
  phaseError: string | null = null;
  /** Appels Team AI / création de phases en chaîne. */
  planningBusy = false;

  loading = true;
  error: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private supervision: ProjectSupervisionService,
    private teamAi: TeamAiService,
    private toast: ToastService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.router.navigate(['/projets']);
      return;
    }
    this.projectId = id;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = p;
        const me = this.auth.getStoredUser();
        if (!me?.userId || p.projectOwnerId !== me.userId) {
          this.error = 'Accès réservé au porteur du projet.';
          this.loading = false;
          return;
        }
        if (p.status !== 'IN_PROGRESS') {
          this.error = 'La supervision est disponible lorsque le projet est IN_PROGRESS.';
          this.loading = false;
          return;
        }
        this.reloadData();
        this.loading = false;
      },
      error: () => {
        this.error = 'Projet introuvable.';
        this.loading = false;
      },
    });
  }

  reloadData(): void {
    this.projectService.getInsights(this.projectId).subscribe({
      next: (i) => (this.insights = i),
      error: () => (this.insights = null),
    });
    this.supervision.listPhases(this.projectId).subscribe({
      next: (list) => {
        this.phases = list ?? [];
        const selId = this.selectedPhase?.id;
        const still = selId != null ? this.phases.find((p) => p.id === selId) : undefined;
        if (still) {
          this.selectedPhase = still;
        } else if (this.phases.length > 0) {
          this.selectedPhase = this.phases[0];
        } else {
          this.selectedPhase = null;
          this.deliverables = [];
          this.deliverablesByPhaseId = {};
          this.copilot = null;
        }
        this.loadDeliverablesForAllPhases();
        if (this.selectedPhase) {
          this.loadCopilot();
        }
      },
      error: () => (this.phases = []),
    });
  }

  /** Charge les livrables de toutes les phases pour l’affichage dans la liste latérale. */
  private loadDeliverablesForAllPhases(): void {
    const phases = this.phases;
    if (phases.length === 0) {
      this.deliverablesByPhaseId = {};
      this.deliverables = [];
      return;
    }
    this.deliverablesLoadingAll = true;
    forkJoin(
      phases.map((ph) =>
        this.supervision.listDeliverables(this.projectId, ph.id).pipe(
          map((list) => [ph.id, list ?? []] as const)
        )
      )
    ).subscribe({
      next: (entries) => {
        const next: Record<number, PhaseDeliverable[]> = {};
        for (const [phaseId, list] of entries) {
          next[phaseId] = list;
        }
        this.deliverablesByPhaseId = next;
        this.deliverablesLoadingAll = false;
        if (this.selectedPhase) {
          this.deliverables = [...(this.deliverablesByPhaseId[this.selectedPhase.id] ?? [])];
        }
      },
      error: () => {
        this.deliverablesLoadingAll = false;
      },
    });
  }

  getDeliverablesForPhase(phaseId: number): PhaseDeliverable[] {
    return this.deliverablesByPhaseId[phaseId] ?? [];
  }

  selectPhase(p: ProjectPhase): void {
    this.selectedPhase = p;
    this.copilot = null;
    this.deliverables = [...(this.deliverablesByPhaseId[p.id] ?? [])];
    this.loadCopilot();
    if (this.deliverablesByPhaseId[p.id] === undefined && !this.deliverablesLoadingAll) {
      this.refreshDeliverablesForPhase(p.id);
    }
  }

  /** Recharge les livrables d’une phase (après ajout / revue) et met à jour la carte latérale. */
  refreshDeliverablesForPhase(phaseId: number): void {
    this.supervision.listDeliverables(this.projectId, phaseId).subscribe({
      next: (d) => {
        const list = d ?? [];
        this.deliverablesByPhaseId = { ...this.deliverablesByPhaseId, [phaseId]: list };
        if (this.selectedPhase?.id === phaseId) {
          this.deliverables = [...list];
        }
      },
    });
  }

  addDeliverable(): void {
    if (!this.selectedPhase || !this.newDeliverableTitle.trim()) return;
    this.savingDeliverable = true;
    this.supervision
      .createDeliverable(this.projectId, this.selectedPhase.id, {
        title: this.newDeliverableTitle.trim(),
        description: this.newDeliverableDescription.trim() || undefined,
        type: this.newDeliverableType,
      })
      .subscribe({
        next: () => {
          this.savingDeliverable = false;
          this.newDeliverableTitle = '';
          this.newDeliverableDescription = '';
          this.refreshDeliverablesForPhase(this.selectedPhase!.id);
          this.loadCopilot();
        },
        error: () => (this.savingDeliverable = false),
      });
  }

  setDeliverableReview(d: PhaseDeliverable, reviewStatus: string): void {
    if (!this.selectedPhase) return;
    this.reviewingId = d.id;
    this.supervision
      .reviewDeliverable(this.projectId, this.selectedPhase.id, d.id, {
        reviewStatus,
        reviewComment: null,
      })
      .subscribe({
        next: () => {
          this.reviewingId = null;
          this.refreshDeliverablesForPhase(this.selectedPhase!.id);
          this.loadCopilot();
        },
        error: () => (this.reviewingId = null),
      });
  }

  loadCopilot(): void {
    if (!this.selectedPhase) return;
    this.copilotLoading = true;
    this.supervision.getDecisionCopilot(this.projectId, this.selectedPhase.id).subscribe({
      next: (c) => {
        this.copilot = c;
        this.copilotLoading = false;
      },
      error: () => {
        this.copilot = null;
        this.copilotLoading = false;
      },
    });
  }

  addPhase(): void {
    if (!this.phaseName.trim()) {
      this.phaseError = 'Nom requis.';
      return;
    }
    this.savingPhase = true;
    this.phaseError = null;
    this.supervision
      .createPhase(this.projectId, {
        name: this.phaseName.trim(),
        description: this.phaseDescription.trim() || undefined,
        phaseOrder: this.phaseOrder,
        startDate: this.phaseStart || null,
        dueDate: this.phaseDue || null,
      })
      .subscribe({
        next: () => {
          this.savingPhase = false;
          this.phaseName = '';
          this.phaseDescription = '';
          this.reloadData();
        },
        error: (err) => {
          this.savingPhase = false;
          this.phaseError = err?.error?.message ?? 'Impossible d’ajouter la phase (ordre déjà utilisé ou règle métier).';
        },
      });
  }

  /**
   * Plan initial : POST Team AI analyze-project → création séquentielle des phases (rôles ou gabarit selon complexité).
   */
  generateInitialPlanFromAi(): void {
    if (!this.project) return;
    if (this.phases.length > 0) {
      this.toast.info(
        'Des phases existent déjà. Supprimez-les ou ajustez manuellement avant de régénérer un plan initial.'
      );
      return;
    }
    const title = this.project.title?.trim() || 'Projet';
    const description = this.project.description?.trim() || title;
    this.planningBusy = true;
    this.teamAi
      .analyzeProject({ title, description })
      .pipe(
        switchMap((res) => {
          const drafts = this.buildPhaseDraftsFromAnalysis(res);
          if (drafts.length === 0) {
            this.toast.error('L’analyse Team AI n’a proposé aucune phase.');
            return EMPTY;
          }
          return from(drafts).pipe(
            concatMap((body) => this.supervision.createPhase(this.projectId, body)),
            toArray()
          );
        }),
        finalize(() => (this.planningBusy = false))
      )
      .subscribe({
        next: (created) => {
          if (!created?.length) return;
          this.toast.success(`${created.length} phase(s) créée(s) à partir de Team AI (proxy /api/team-ai).`);
          this.reloadData();
        },
        error: (err) => this.toast.error(this.formatPlanningError(err)),
      });
  }

  /**
   * Réajustement : score ML projet + nouvelle analyse ; si risque élevé, ajoute une phase d’atténuation.
   */
  adjustPlanForRisk(): void {
    if (!this.project) return;
    const title = this.project.title?.trim() || 'Projet';
    const description = this.project.description?.trim() || title;
    this.planningBusy = true;
    forkJoin({
      risk: this.projectService.getMlRisk(this.projectId).pipe(
        catchError((): Observable<ProjectMlRisk> =>
          of<ProjectMlRisk>({
            projectId: this.projectId,
            riskScore0To100: 0,
            riskLevel: 'UNKNOWN',
            probabilityHighRisk: 0,
            flags: [],
            summary: 'Score ML indisponible.',
            modelId: '',
            heuristicFallback: true,
          })
        )
      ),
      analysis: this.teamAi.analyzeProject({ title, description }),
    })
      .pipe(
        switchMap(({ risk, analysis }) => {
          const score = risk.riskScore0To100 ?? 0;
          const high = score >= 55;
          const nextOrder =
            this.phases.length > 0 ? Math.max(...this.phases.map((p) => p.phaseOrder)) + 1 : 1;
          if (!high) {
            this.toast.success(
              `Risque modéré (${score}/100 — ${risk.riskLevel}). ${(risk.summary || '').slice(0, 160)}${
                (risk.summary?.length ?? 0) > 160 ? '…' : ''
              }`
            );
            return of(null);
          }
          const desc = [
            `Contexte ML : ${risk.summary}`,
            analysis.summary ? `Synthèse IA : ${analysis.summary}` : null,
            analysis.complexity ? `Complexité : ${analysis.complexity}` : null,
          ]
            .filter(Boolean)
            .join('\n\n')
            .slice(0, 2000);
          const body: CreatePhaseBody = {
            name: 'Renforcement & atténuation des risques',
            description: desc || undefined,
            phaseOrder: nextOrder,
            startDate: null,
            dueDate: null,
          };
          return this.supervision.createPhase(this.projectId, body).pipe(
            tap(() => {
              this.toast.success(
                `Risque élevé (${score}/100). Phase d’atténuation ajoutée (ordre ${nextOrder}).`
              );
              this.reloadData();
            })
          );
        }),
        finalize(() => (this.planningBusy = false))
      )
      .subscribe({
        error: (err) => this.toast.error(this.formatPlanningError(err)),
      });
  }

  private buildPhaseDraftsFromAnalysis(res: AnalyzeProjectResponse): CreatePhaseBody[] {
    const projectDur = this.project?.duration ?? 30;
    const totalDays = Math.max(1, res.durationEstimateDays ?? projectDur);
    let names = (res.roles ?? [])
      .map((r) => String(r).trim())
      .filter((s) => s.length > 0);
    names = [...new Set(names)];
    if (names.length === 0) {
      names = this.defaultPhaseNamesForComplexity(res.complexity);
    }
    names = names.slice(0, 6);
    const n = names.length;
    const chunk = Math.max(1, Math.floor(totalDays / n));
    const today = new Date();
    return names.map((name, i) => {
      const start = new Date(today);
      start.setDate(start.getDate() + i * chunk);
      const due = new Date(start);
      due.setDate(due.getDate() + Math.max(0, chunk - 1));
      return {
        name,
        description: this.phaseDescriptionFromAnalysis(res, i),
        phaseOrder: i + 1,
        startDate: start.toISOString().slice(0, 10),
        dueDate: due.toISOString().slice(0, 10),
      };
    });
  }

  private defaultPhaseNamesForComplexity(complexity: string): string[] {
    const c = (complexity || 'medium').toLowerCase();
    if (c === 'simple') {
      return ['Cadrage', 'Réalisation', 'Recette utilisateur'];
    }
    if (c === 'complex') {
      return [
        'Discovery',
        'Architecture & design',
        'Développement',
        'QA & sécurité',
        'Mise en production',
      ];
    }
    return ['Analyse & cadrage', 'Conception', 'Développement', 'Tests & livraison'];
  }

  private phaseDescriptionFromAnalysis(res: AnalyzeProjectResponse, index: number): string {
    const parts = [
      res.summary?.trim(),
      res.complexity ? `Complexité : ${res.complexity}` : null,
      res.technicalLeaderRole ? `Rôle clé : ${res.technicalLeaderRole}` : null,
    ].filter(Boolean) as string[];
    const base = parts.length ? parts.join(' — ') : 'Phase proposée par l’analyse Team AI.';
    return index === 0 ? base : `${base} (volet ${index + 1})`;
  }

  private formatPlanningError(err: unknown): string {
    const e = err as { error?: { detail?: unknown; message?: string }; message?: string };
    const d = e?.error?.detail;
    if (typeof d === 'string') return d;
    if (Array.isArray(d)) {
      return (
        d
          .map((x: { msg?: string }) => x?.msg)
          .filter(Boolean)
          .join(' — ') || 'Erreur Team AI.'
      );
    }
    return (
      e?.error?.message ??
      e?.message ??
      'Service Team AI ou Project indisponible (Gateway, ports 5000 / 8050).'
    );
  }

  closeSelectedPhase(): void {
    if (!this.selectedPhase) return;
    this.closingPhase = true;
    this.supervision.closePhase(this.projectId, this.selectedPhase.id).subscribe({
      next: () => {
        this.closingPhase = false;
        this.reloadData();
      },
      error: () => (this.closingPhase = false),
    });
  }

  /** % temps calendaire écoulé depuis création du projet. */
  get elapsedTimelinePercent(): number {
    if (!this.project?.createdAt || !this.project.duration) return 0;
    const start = new Date(this.project.createdAt).getTime();
    const days = (Date.now() - start) / 86400000;
    return Math.min(100, Math.round((days / Math.max(1, this.project.duration)) * 100));
  }

  /** Avancement basé sur les phases approuvées. */
  get progressFromPhasesPercent(): number {
    if (!this.phases?.length) return 0;
    const done = this.phases.filter((p) => p.status === 'APPROVED').length;
    return Math.round((done / this.phases.length) * 100);
  }

  get ecartPts(): number {
    return this.elapsedTimelinePercent - this.progressFromPhasesPercent;
  }

  get phasesLateCount(): number {
    const now = Date.now();
    return (this.phases ?? []).filter((p) => {
      if (!p.dueDate || p.status === 'APPROVED') return false;
      return new Date(p.dueDate).getTime() < now;
    }).length;
  }

  planningBannerOk(): boolean {
    const h = this.insights?.compositeHealthScore ?? 0;
    return h >= 45 && this.phasesLateCount === 0;
  }
}
