import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { TeamAiService } from '../../services/team-ai.service';
import { CompatibilityNotifierService } from '../../services/compatibility-notifier.service';
import { NotificationService } from '../../services/notification.service';
import { getBackendErrorMessage } from '../../utils/http-error.util';
import { ProjectRequest, ProjectStatus } from '../../models/project.model';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './project-form.component.html',
})
export class ProjectFormComponent implements OnInit {
  isEdit = false;
  projectId?: number;
  saving = false;
  error = '';
  analyzeLoading = false;

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

  onSubmit() {
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
          this.router.navigate(['/projects', p.id]);
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
          this.toast.error(msg || 'Impossible de contacter le service de compatibilité. Vérifiez que le service team-ai (port 5000) est démarré.');
        },
      });
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
        this.toast.error('Service Team AI indisponible. Vérifiez que le service Python est lancé (port 5000).');
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

