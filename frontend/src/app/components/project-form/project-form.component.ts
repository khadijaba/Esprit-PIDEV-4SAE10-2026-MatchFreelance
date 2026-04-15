import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { TeamAiService } from '../../services/team-ai.service';
import { CompatibilityNotifierService } from '../../services/compatibility-notifier.service';
import { NotificationService } from '../../services/notification.service';
import { getBackendErrorMessage, httpErrorMessage } from '../../utils/http-error.util';
import { ProjectEffortEstimate } from '../../models/project-effort.model';
import { ProjectMlRisk } from '../../models/project-ml-risk.model';
import { ProjectRequest, ProjectStatus } from '../../models/project.model';
import { DescriptionCoachResponse } from '../../models/team-ai.model';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-form.component.html',
})
export class ProjectFormComponent implements OnInit {
  @ViewChild('f') formDir?: NgForm;

  isEdit = false;
  projectId?: number;
  saving = false;
  error = '';
  analyzeLoading = false;
  descCoachLoading = false;
  descCoach: DescriptionCoachResponse | null = null;
  descCoachHelpOpen = false;
  descCoachUseLlm = false;
  descCoachPreviewTab: 'rules' | 'llm' = 'rules';
  effort?: ProjectEffortEstimate;
  effortLoading = false;
  effortError: string | null = null;
  mlRisk?: ProjectMlRisk;
  mlRiskLoading = false;
  mlRiskError: string | null = null;
  /** Id du brouillon en base (créé à la fin de l’étape 1 pour appeler charge / risque ML). */
  draftProjectId?: number;
  draftSaving = false;
  /** Case à cocher obligatoire avant publication après lecture des indicateurs. */
  ownerConfirmReviewed = false;
  /**
   * Création côté Project Owner : étape 1 = saisie, étape 2 = charge + risque puis publication OPEN.
   */
  ownerCreateStep: 1 | 2 = 1;

  form: ProjectRequest = {
    title: '',
    description: '',
    budget: 0,
    duration: 1,
    status: 'OPEN',
    projectOwnerId: 0,
    requiredSkills: [],
  };

  newSkill = '';

  addSkill() {
    if (this.newSkill.trim() && !this.form.requiredSkills?.includes(this.newSkill.trim())) {
      if (!this.form.requiredSkills) {
        this.form.requiredSkills = [];
      }
      this.form.requiredSkills.push(this.newSkill.trim());
      this.newSkill = '';
    }
  }

  removeSkill(skill: string) {
    if (this.form.requiredSkills) {
      this.form.requiredSkills = this.form.requiredSkills.filter(s => s !== skill);
    }
  }

  statuses: { value: ProjectStatus; label: string }[] = [
    { value: 'DRAFT', label: 'Draft' },
    { value: 'OPEN', label: 'Open' },
    { value: 'IN_PROGRESS', label: 'In Progress' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  constructor(
    private projectService: ProjectService,
    private toast: ToastService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private teamAi: TeamAiService,
    private compatibilityNotifier: CompatibilityNotifierService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    const currentUserId = this.auth.getCurrentUser()?.id != null ? Number(this.auth.getCurrentUser()!.id) : 0;
    if (window.location.pathname.includes('/project-owner') && !this.route.snapshot.paramMap.get('id')) {
      this.form.projectOwnerId = currentUserId;
      const draftQ = this.route.snapshot.queryParamMap.get('draft');
      if (draftQ && this.isOwnerCreateWizard) {
        const did = +draftQ;
        if (Number.isFinite(did) && did > 0) {
          this.ownerCreateStep = 2;
          this.hydrateDraftFromQuery(did);
        } else {
          this.ownerCreateStep = 1;
        }
      } else {
        this.ownerCreateStep = 1;
      }
    }

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.projectId = +id;
      this.projectService.getById(this.projectId).subscribe({
        next: (p) => {
          this.form = {
            title: p.title,
            description: p.description,
            budget: p.budget,
            duration: p.duration,
            status: p.status,
            projectOwnerId: p.projectOwnerId,
            requiredSkills: p.requiredSkills || [],
          };
          this.loadEffortEstimate();
        },
        error: () => {
          const isProjectOwner = window.location.pathname.includes('/project-owner');
          if (isProjectOwner) {
            this.router.navigate(['/project-owner/projects']);
          } else {
            this.router.navigate(['/admin/projects']);
          }
        },
      });
    }
  }

  reloadEffortEstimate(): void {
    this.effortError = null;
    this.effort = undefined;
    this.loadEffortEstimate();
  }

  /** Charge estimée (jours-homme) — édition uniquement, basée sur le projet en base. */
  private loadEffortEstimate(): void {
    if (!this.projectId) return;
    this.effortLoading = true;
    this.effortError = null;
    this.projectService.getEffortEstimate(this.projectId).subscribe({
      next: (e) => {
        this.effort = e;
        this.effortLoading = false;
      },
      error: (err: unknown) => {
        this.effortLoading = false;
        this.effort = undefined;
        this.effortError = httpErrorMessage(
          err,
          'Impossible de charger la charge estimée. Vérifiez le microservice Project, Eureka et la gateway (8086).'
        );
        this.toast.error(this.effortError);
      },
    });
  }

  onSubmit() {
    if (this.isOwnerCreateWizard && this.ownerCreateStep === 1) {
      const f = this.formDir;
      if (f) this.saveDraftAndOpenVerification(f);
      return;
    }

    if (this.isOwnerCreateWizard && this.ownerCreateStep === 2) {
      this.publishOwnerDraft();
      return;
    }

    const titleErr = this.getTitleValidationError();
    if (titleErr) {
      this.error = titleErr;
      this.toast.error(titleErr);
      return;
    }
    this.saving = true;
    this.error = '';

    const obs = this.isEdit
      ? this.projectService.update(this.projectId!, this.form)
      : this.projectService.create(this.form);

    obs.subscribe({
      next: (p) => {
        this.saving = false;
        this.toast.success(this.isEdit ? 'Project updated successfully' : 'Project created successfully');
        const raw = (p.requiredSkills?.length ? p.requiredSkills : this.form.requiredSkills) ?? [];
        const requiredSkills = raw
          .flatMap((s) => (typeof s === 'string' ? s : String(s)).split(',').map((x) => x.trim()))
          .filter(Boolean);
        if (!this.isEdit && p.id && requiredSkills.length > 0) {
          this.notifyCompatibleFreelancers(p.id, p.title ?? this.form.title, requiredSkills);
        }
        const isProjectOwner = window.location.pathname.includes('/project-owner');
        if (isProjectOwner) {
          this.router.navigate(['/project-owner/projects', p.id]);
        } else {
          this.router.navigate(['/admin/projects', p.id]);
        }
      },
      error: (err) => {
        this.saving = false;
        this.error = getBackendErrorMessage(err, 'Une erreur est survenue. Vérifiez vos données.');
        this.toast.error(this.error);
      },
    });
  }

  isProjectOwnerRoute(): boolean {
    return window.location.pathname.includes('/project-owner');
  }

  /** Création d’un nouveau projet depuis l’espace Project Owner (wizard estimation → publication). */
  get isOwnerCreateWizard(): boolean {
    return !this.isEdit && this.isProjectOwnerRoute();
  }

  /**
   * Fin étape 1 : enregistre un brouillon (DRAFT) pour obtenir un id, puis affiche charge + risque ML.
   */
  saveDraftAndOpenVerification(f: NgForm): void {
    const titleErr = this.getTitleValidationError();
    if (titleErr) {
      this.error = titleErr;
      this.toast.error(titleErr);
      return;
    }
    if (f.invalid) {
      this.toast.error('Complétez titre, description, budget et durée estimée.');
      return;
    }
    const ownerId = Number(this.form.projectOwnerId);
    if (!Number.isFinite(ownerId) || ownerId <= 0) {
      this.toast.error('Session invalide : reconnectez-vous en tant que porteur de projet pour enregistrer le brouillon.');
      return;
    }
    this.error = '';
    this.draftSaving = true;
    const body: ProjectRequest = {
      ...this.form,
      status: 'DRAFT',
      projectOwnerId: this.form.projectOwnerId,
    };
    const obs = this.draftProjectId
      ? this.projectService.update(this.draftProjectId, body)
      : this.projectService.create(body);
    obs.subscribe({
      next: (p) => {
        this.draftSaving = false;
        this.draftProjectId = p.id;
        this.form.status = 'DRAFT';
        this.ownerConfirmReviewed = false;
        this.ownerCreateStep = 2;
        this.router.navigate(['/project-owner/projects/new'], {
          queryParams: { draft: p.id },
          replaceUrl: true,
        });
        this.loadDraftInsights();
        this.toast.success('Brouillon enregistré. Vérifiez la charge et le risque ; corrigez si besoin avant publication.');
      },
      error: (err) => {
        this.draftSaving = false;
        this.error = getBackendErrorMessage(err, 'Impossible d’enregistrer le brouillon.');
        this.toast.error(this.error);
      },
    });
  }

  goBackToEstimateStep(): void {
    this.ownerCreateStep = 1;
    this.ownerConfirmReviewed = false;
  }

  reloadDraftInsights(): void {
    this.ownerConfirmReviewed = false;
    this.loadDraftInsights();
  }

  private hydrateDraftFromQuery(id: number): void {
    const uid = this.auth.getCurrentUser()?.id;
    if (uid == null) return;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        if (Number(p.projectOwnerId) !== Number(uid)) {
          this.ownerCreateStep = 1;
          this.draftProjectId = undefined;
          this.toast.error('Ce brouillon ne vous appartient pas.');
          void this.router.navigate(['/project-owner/projects']);
          return;
        }
        if (p.status !== 'DRAFT') {
          this.ownerCreateStep = 1;
          this.draftProjectId = undefined;
          this.toast.show('Ce projet n’est plus un brouillon.', 'info');
          void this.router.navigate(['/project-owner/projects', p.id]);
          return;
        }
        this.draftProjectId = id;
        this.form = {
          title: p.title,
          description: p.description,
          budget: p.budget,
          duration: p.duration,
          status: 'DRAFT',
          projectOwnerId: p.projectOwnerId,
          requiredSkills: p.requiredSkills || [],
        };
        this.ownerCreateStep = 2;
        this.ownerConfirmReviewed = false;
        this.loadDraftInsights();
      },
      error: () => {
        this.ownerCreateStep = 1;
        this.draftProjectId = undefined;
        this.toast.error('Brouillon introuvable.');
        void this.router.navigate(['/project-owner/projects/new'], { replaceUrl: true });
      },
    });
  }

  private loadDraftInsights(): void {
    const id = this.draftProjectId;
    if (!id) return;
    this.effort = undefined;
    this.effortError = null;
    this.mlRisk = undefined;
    this.mlRiskError = null;
    this.effortLoading = true;
    this.mlRiskLoading = true;
    this.projectService.getEffortEstimate(id).subscribe({
      next: (e) => {
        this.effort = e;
        this.effortLoading = false;
      },
      error: (err: unknown) => {
        this.effortLoading = false;
        this.effort = undefined;
        this.effortError = httpErrorMessage(err, 'Charge estimée indisponible.');
      },
    });
    this.projectService.getMlRisk(id).subscribe({
      next: (r) => {
        this.mlRisk = r;
        this.mlRiskLoading = false;
      },
      error: (err: unknown) => {
        this.mlRiskLoading = false;
        this.mlRisk = undefined;
        this.mlRiskError = httpErrorMessage(err, 'Score de risque ML indisponible.');
      },
    });
  }

  private publishOwnerDraft(): void {
    const titleErr = this.getTitleValidationError();
    if (titleErr) {
      this.error = titleErr;
      this.toast.error(titleErr);
      return;
    }
    if (!this.draftProjectId) {
      this.toast.error('Brouillon manquant. Recommencez depuis l’étape 1.');
      return;
    }
    if (!this.ownerConfirmReviewed) {
      this.toast.error('Cochez la confirmation après avoir relu la charge et le risque (ou corrigé le projet).');
      return;
    }
    if (this.effortLoading || this.mlRiskLoading) {
      this.toast.error('Patientez jusqu’au chargement des indicateurs (ou utilisez Retour pour corriger).');
      return;
    }
    this.saving = true;
    this.error = '';
    const body: ProjectRequest = {
      ...this.form,
      status: 'OPEN',
      projectOwnerId: this.form.projectOwnerId,
    };
    this.projectService.update(this.draftProjectId, body).subscribe({
      next: (p) => {
        this.saving = false;
        this.toast.success('Projet publié.');
        const raw = (p.requiredSkills?.length ? p.requiredSkills : this.form.requiredSkills) ?? [];
        const requiredSkills = raw
          .flatMap((s) => (typeof s === 'string' ? s : String(s)).split(',').map((x) => x.trim()))
          .filter(Boolean);
        if (p.id && requiredSkills.length > 0) {
          this.notifyCompatibleFreelancers(p.id, p.title ?? this.form.title, requiredSkills);
        }
        void this.router.navigate(['/project-owner/projects', p.id]);
      },
      error: (err) => {
        this.saving = false;
        this.error = getBackendErrorMessage(err, 'Impossible de publier le projet.');
        this.toast.error(this.error);
      },
    });
  }

  mlRiskBadgeClass(level: string): string {
    const map: Record<string, string> = {
      LOW: 'bg-emerald-100 text-emerald-800',
      MEDIUM: 'bg-amber-100 text-amber-800',
      HIGH: 'bg-red-100 text-red-800',
    };
    return map[level] ?? 'bg-gray-100 text-gray-800';
  }

  private notifyCompatibleFreelancers(projectId: number, projectTitle: string, requiredSkills: string[]): void {
    this.compatibilityNotifier
      .computeCompatibleFreelancers({ projectId, projectTitle, requiredSkills })
      .subscribe({
        next: (res) => {
          const list = res?.notifications ?? [];
          if (list.length > 0) {
            const toSend = list.map((n) => ({
              freelancerId: n.freelancerId,
              email: n.email,
              subject: n.subject ?? 'Nouveau projet compatible pour vous !',
              message: n.message,
            }));
            this.notificationService.sendBulk(projectId, projectTitle, toSend);
            this.toast.success(
              `${list.length} freelancer(s) compatible(s) notifié(s). Ils verront la notification dans leur espace.`
            );
          } else {
            this.toast.show(
              'Aucun freelancer avec un score de compatibilité ≥ 70 % pour ce projet. Les notifications seront envoyées quand des profils correspondants existeront.',
              'info'
            );
          }
        },
        error: (err) => {
          const msg = typeof err?.error?.detail === 'string' ? err.error.detail : err?.error?.message || err?.message;
          const hint =
            msg ||
            'Service Team AI (Python, port 5000) indisponible — notifications automatiques ignorées. Le projet est bien enregistré.';
          this.toast.show(hint, 'info');
        },
      });
  }

  /** Suggestions de reformulation (périmètre, livrables, critères d’acceptation) — brouillon à appliquer ou ignorer. */
  onDescriptionCoach(): void {
    const title = this.form.title?.trim();
    const desc = this.form.description?.trim();
    if (!title || !desc) {
      this.toast.error('Saisissez un titre et une description pour obtenir des suggestions.');
      return;
    }
    this.descCoachLoading = true;
    this.descCoach = null;
    this.descCoachPreviewTab = 'rules';
    this.teamAi.descriptionCoach({ title, description: desc, useLlm: this.descCoachUseLlm }).subscribe({
      next: (res) => {
        this.descCoachLoading = false;
        this.descCoach = res;
               if (res.llmUsed && res.llmEnrichedMarkdown) {
          this.descCoachPreviewTab = 'llm';
        }
        if (this.descCoachUseLlm && !res.llmUsed) {
          this.toast.show(
            'LLM non appelé : lancez Ollama en local (ollama.com), exécutez « ollama pull llama3.2 » (ou le modèle défini par OLLAMA_MODEL), vérifiez http://127.0.0.1:11434 — ou définissez OPENAI_API_KEY. Variable LLM_PROVIDER=auto|ollama|openai sur le serveur Team AI.',
            'info'
          );
        }
        this.toast.success(res.summary ?? 'Suggestions générées. Relisez le brouillon avant d’appliquer.');
      },
      error: (err: HttpErrorResponse) => {
        this.descCoachLoading = false;
        const hint = httpErrorMessage(
          err,
          'Service de reformulation indisponible. Vérifiez Team AI (port 5000), gateway team.ai.url, endpoint POST /api/analyze-description (alias de description-coach).'
        );
        this.toast.show(hint, 'info');
      },
    });
  }

  applyDescriptionDraft(): void {
    if (!this.descCoach) return;
    const fromLlm =
      this.descCoachPreviewTab === 'llm' &&
      this.descCoach.llmUsed &&
      (this.descCoach.llmEnrichedMarkdown?.trim().length ?? 0) > 0;
    let text = fromLlm ? (this.descCoach.llmEnrichedMarkdown as string) : this.descCoach.draftEnrichedDescription;
    text = text.trim();
    if (text.length > 2000) {
      text = text.slice(0, 2000);
      this.toast.show('Texte tronqué à 2000 caractères (limite du formulaire).', 'info');
    }
    this.form.description = text;
    this.descCoach = null;
    this.toast.success('Description mise à jour — relisez et ajustez avant publication.');
  }

  dismissDescriptionCoach(): void {
    this.descCoach = null;
  }

  /** Analyse la description avec l’IA (NLP) et pré-remplit compétences, budget, durée. */
  onAnalyzeWithAI(): void {
    const title = this.form.title?.trim();
    const desc = this.form.description?.trim();
    if (!title || !desc) {
      this.toast.error('Saisissez un titre et une description pour lancer l’analyse.');
      return;
    }
    this.analyzeLoading = true;
    this.teamAi.analyzeProject({ title, description: desc }).subscribe({
      next: (res) => {
        this.analyzeLoading = false;
        if (res.requiredSkills?.length) {
          this.form.requiredSkills = [...(this.form.requiredSkills ?? []), ...res.requiredSkills];
          this.form.requiredSkills = [...new Set(this.form.requiredSkills)];
        }
        if (res.budgetEstimate && res.budgetEstimate.minAmount > 0) {
          this.form.budget = Math.round(res.budgetEstimate.minAmount);
        }
        if (res.durationEstimateDays && res.durationEstimateDays > 0) {
          this.form.duration = res.durationEstimateDays;
        }
        this.toast.success(res.summary ?? 'Analyse terminée : compétences, budget et durée mis à jour.');
      },
      error: () => {
        this.analyzeLoading = false;
        this.toast.show(
          'Analyse IA indisponible. Lancez le service Python sur le port 5000 (proxy /api/ai) ou remplissez le formulaire manuellement.',
          'info'
        );
      },
    });
  }

  /**
   * Vérifie que le titre est réaliste : au moins 2 mots, pas uniquement ponctuation ou caractères répétés.
   * Ex. refusé : "gjgfdhgfhgdh", ",,,,,;;; , hhhhh"
   */
  getTitleValidationError(): string | null {
    const t = (this.form.title ?? '').trim();
    if (t.length < 3) return null;
    const words = t.split(/\s+/).filter((w) => w.length > 0);
    if (words.length < 2) return 'Le titre doit contenir au moins deux mots (ex. Site web e-commerce).';
    const wordsMinLength = words.filter((w) => w.replace(/[^\p{L}]/gu, '').length >= 2);
    if (wordsMinLength.length < 2) return 'Chaque mot doit contenir au moins 2 lettres.';
    const letters = (t.match(/[\p{L}]/gu) ?? []).join('');
    const withoutSpaces = t.replace(/\s/g, '');
    if (withoutSpaces.length < 3) return 'Le titre doit contenir au moins 3 caractères utiles.';
    const letterRatio = letters.length / withoutSpaces.length;
    if (letterRatio < 0.5) return 'Le titre doit être principalement du texte (lettres), pas des symboles ou ponctuation.';
    if (/(.)\1{3,}/.test(t)) return 'Le titre ne doit pas contenir de caractères répétés (ex. hhhhh).';
    if (/[,;.\-_!\?]{4,}/.test(t)) return 'Le titre ne doit pas contenir une suite de ponctuation (ex. ,,,, ;;;;).';
    return null;
  }
}

