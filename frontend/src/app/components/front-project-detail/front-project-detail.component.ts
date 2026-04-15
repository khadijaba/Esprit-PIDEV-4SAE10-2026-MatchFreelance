import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { filter, map, switchMap } from 'rxjs/operators';
import { ProjectService } from '../../services/project.service';
import { TalentMatchingService } from '../../services/talent-matching.service';
import { SkillGapService } from '../../services/skill-gap.service';
import { SkillService } from '../../services/skill.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { CandidatureService } from '../../services/candidature.service';
import { DiscussionNavigationService } from '../../services/discussion-navigation.service';
import { normalizeProjectRequiredSkills } from '../../utils/project-skill.util';
import { Project, ProjectStatus } from '../../models/project.model';
import { MatchingFreelancer } from '../../models/matching.model';
import { SkillGapResult } from '../../models/skill-gap.model';
import { Candidature } from '../../models/candidature.model';
import { ProjectEffortEstimate } from '../../models/project-effort.model';
import { ProjectMlRisk } from '../../models/project-ml-risk.model';
import { httpErrorMessage } from '../../utils/http-error.util';

@Component({
  selector: 'app-front-project-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './front-project-detail.component.html',
})
export class FrontProjectDetailComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  project?: Project;
  loading = true;
  matchingFreelancers: MatchingFreelancer[] = [];
  matchingLoading = false;
  /** Skill Gap Analyzer : affiché uniquement pour un freelancer connecté */
  skillGap: SkillGapResult | null = null;
  skillGapLoading = false;
  /** Candidature du freelancer connecté sur ce projet (si existe) */
  myCandidature: Candidature | null = null;
  applyMessage = '';
  applyBudget: number | null = null;
  applyExtra: number | null = null;
  applySubmitting = false;
  mlRisk?: ProjectMlRisk;
  mlRiskLoading = false;
  effort?: ProjectEffortEstimate;
  effortLoading = false;
  /** Message si l’API effort-estimate échoue (sinon la section disparaît sans explication). */
  effortError: string | null = null;

  constructor(
    private projectService: ProjectService,
    private matchingService: TalentMatchingService,
    private skillGapService: SkillGapService,
    private skillService: SkillService,
    private auth: AuthService,
    private toast: ToastService,
    private candidatureService: CandidatureService,
    private discussionNav: DiscussionNavigationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  openDiscussion(freelancerId: number, name: string): void {
    if (name?.trim()) this.discussionNav.setPendingName(name.trim());
    this.router.navigate(['/discussion', freelancerId]);
  }

  get isFreelancer(): boolean {
    return this.auth.isFreelancer();
  }

  get isProjectOwner(): boolean {
    return this.auth.isProjectOwner();
  }

  /** Propriétaire du projet (même compte que projectOwnerId) */
  get isOwnerOfThisProject(): boolean {
    const u = this.auth.getCurrentUser();
    return !!(
      this.project &&
      u?.id != null &&
      Number(this.project.projectOwnerId) === Number(u.id)
    );
  }

  ngOnInit() {
    this.loading = true;
    this.route.paramMap
      .pipe(
        map((pm) => +pm.get('id')!),
        filter((id) => Number.isFinite(id) && id > 0),
        switchMap((id) => this.projectService.getById(id)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (p) => {
          if (p.status === 'DRAFT') {
            const u = this.auth.getCurrentUser();
            const isOwner = u?.id != null && Number(p.projectOwnerId) === Number(u.id);
            if (!isOwner) {
              this.toast.error('Ce projet n’est pas encore publié.');
              this.router.navigate(['/projects']);
              return;
            }
          }
          this.project = normalizeProjectRequiredSkills(p);
          this.loading = false;
          this.applyBudget = p.budget ?? null;
          this.loadMatching();
          this.loadSkillGap();
          this.loadMyCandidature();
          this.loadMlRisk();
          this.loadEffort();
        },
        error: () =>
          this.router.navigate([
            window.location.pathname.includes('/project-owner/projects') ? '/project-owner/projects' : '/projects',
          ]),
      });
  }

  private loadMyCandidature(): void {
    if (!this.isFreelancer || !this.project?.id) return;
    const u = this.auth.getCurrentUser();
    if (u?.id == null) return;
    void this.auth.ensureFreshTokenIfNeeded();
    this.candidatureService.listByFreelancer(Number(u.id)).subscribe({
      next: (list) => {
        this.myCandidature = list.find((c) => c.projectId === this.project!.id) ?? null;
      },
      error: () => {
        this.myCandidature = null;
      },
    });
  }

  submitApplication(): void {
    if (!this.project?.id || !this.isFreelancer) return;
    const u = this.auth.getCurrentUser();
    if (u?.id == null) {
      this.toast.error('Connectez-vous pour postuler.');
      return;
    }
    const budget = this.applyBudget;
    if (budget == null || budget <= 0) {
      this.toast.error('Indiquez un budget proposé valide.');
      return;
    }
    this.applySubmitting = true;
    void this.auth.ensureFreshTokenIfNeeded();
    this.candidatureService
      .create({
        projectId: this.project.id,
        freelancerId: Number(u.id),
        message: this.applyMessage || undefined,
        proposedBudget: budget,
        extraTasksBudget: this.applyExtra != null && this.applyExtra >= 0 ? this.applyExtra : undefined,
      })
      .subscribe({
        next: () => {
          this.applySubmitting = false;
          this.toast.success('Candidature envoyée.');
          this.loadMyCandidature();
        },
        error: (err: unknown) => {
          this.applySubmitting = false;
          this.toast.error(httpErrorMessage(err, 'Impossible d’envoyer la candidature.'));
        },
      });
  }

  reloadEffort(): void {
    this.effortError = null;
    this.effort = undefined;
    this.loadEffort();
  }

  private loadEffort(): void {
    if (!this.project?.id) return;
    this.effortLoading = true;
    this.effortError = null;
    this.projectService.getEffortEstimate(this.project.id).subscribe({
      next: (e) => {
        this.effort = e;
        this.effortLoading = false;
      },
      error: (err: unknown) => {
        this.effortLoading = false;
        this.effort = undefined;
        this.effortError = httpErrorMessage(
          err,
          'Impossible de charger la charge estimée. Vérifiez que le microservice Project et la gateway (8086) tournent, puis réessayez.'
        );
        this.toast.error(this.effortError);
      },
    });
  }

  private loadMlRisk(): void {
    if (!this.project?.id) return;
    this.mlRiskLoading = true;
    this.projectService.getMlRisk(this.project.id).subscribe({
      next: (r) => {
        this.mlRisk = r;
        this.mlRiskLoading = false;
      },
      error: () => {
        this.mlRiskLoading = false;
      },
    });
  }

  private loadMatching(): void {
    if (!this.project?.id) return;
    this.matchingLoading = true;
    this.matchingService.getTopMatchingByProjectId(this.project.id).subscribe({
      next: (list) => {
        this.matchingFreelancers = list;
        this.matchingLoading = false;
      },
      error: (err: unknown) => {
        this.matchingLoading = false;
        this.matchingFreelancers = [];
        this.toast.error(
          httpErrorMessage(err, 'Impossible de charger les profils compatibles. Vérifiez le service Utilisateurs.')
        );
      },
    });
  }

  /**
   * Charge les compétences du freelancer (espace freelancer) et calcule la compatibilité
   * avec les compétences requises du projet (requiredSkills du projet).
   */
  loadSkillGap(): void {
    if (!this.project || !this.isFreelancer) return;
    const user = this.auth.getCurrentUser();
    const userId = user?.id != null ? Number(user.id) : null;
    if (userId == null) return;
    this.skillGapLoading = true;
    this.skillService.getByFreelancerId(userId).subscribe({
      next: (freelancerSkillsFromProfile) => {
        this.skillGap = this.skillGapService.analyze(this.project!, freelancerSkillsFromProfile);
        this.skillGapLoading = false;
      },
      error: () => {
        this.skillGapLoading = false;
      },
    });
  }

  statusClass(status: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      DRAFT: 'bg-slate-200 text-slate-800',
      OPEN: 'bg-emerald-100 text-emerald-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-slate-100 text-slate-600',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-slate-100 text-slate-600';
  }

  formatStatus(status: ProjectStatus): string {
    return status.replace('_', ' ');
  }

  mlRiskBadgeClass(level: string): string {
    const map: Record<string, string> = {
      LOW: 'bg-emerald-100 text-emerald-800',
      MEDIUM: 'bg-amber-100 text-amber-800',
      HIGH: 'bg-red-100 text-red-800',
    };
    return map[level] ?? 'bg-gray-100 text-gray-800';
  }
}
