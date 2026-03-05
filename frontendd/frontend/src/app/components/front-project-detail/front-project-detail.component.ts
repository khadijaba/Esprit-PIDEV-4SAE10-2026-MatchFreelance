import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { TalentMatchingService } from '../../services/talent-matching.service';
import { SkillGapService } from '../../services/skill-gap.service';
import { SkillService } from '../../services/skill.service';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { TeamAiService } from '../../services/team-ai.service';
import { ToastService } from '../../services/toast.service';
import { NotificationService } from '../../services/notification.service';
import { DiscussionNavigationService } from '../../services/discussion-navigation.service';
import type { BuildTeamResponse, FreelancerForTeam, ProjectAnalysisForTeam } from '../../models/team-ai.model';
import { normalizeProjectRequiredSkills } from '../../utils/project-skill.util';
import { Project, ProjectStatus } from '../../models/project.model';
import { MatchingFreelancer } from '../../models/matching.model';
import { SkillGapResult } from '../../models/skill-gap.model';

@Component({
  selector: 'app-front-project-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './front-project-detail.component.html',
})
export class FrontProjectDetailComponent implements OnInit {
  project?: Project;
  loading = true;
  matchingFreelancers: MatchingFreelancer[] = [];
  matchingLoading = false;
  /** Skill Gap Analyzer : affiché uniquement pour un freelancer connecté */
  skillGap: SkillGapResult | null = null;
  skillGapLoading = false;
  /** Équipe proposée par l’IA (project owner) */
  teamResult: BuildTeamResponse | null = null;
  buildTeamLoading = false;

  constructor(
    private projectService: ProjectService,
    private matchingService: TalentMatchingService,
    private skillGapService: SkillGapService,
    private skillService: SkillService,
    private userService: UserService,
    private auth: AuthService,
    private teamAi: TeamAiService,
    private toast: ToastService,
    private notificationService: NotificationService,
    private discussionNav: DiscussionNavigationService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  openDiscussion(freelancerId: number, name: string): void {
    if (name?.trim()) this.discussionNav.setPendingName(name.trim());
    this.router.navigate(['/discussion', freelancerId]);
  }

  getTeamMemberName(freelancerId: number): string {
    const m = this.teamResult?.team?.find((t) => t.freelancerId === freelancerId);
    return m ? `Freelancer ${m.role}` : '';
  }

  get isFreelancer(): boolean {
    return this.auth.isFreelancer();
  }

  get isProjectOwner(): boolean {
    return this.auth.isProjectOwner();
  }

  ngOnInit() {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.projectService.getById(id).subscribe({
      next: (p) => {
        this.project = normalizeProjectRequiredSkills(p);
        this.loading = false;
        this.loadMatching();
        this.loadSkillGap();
      },
      error: () => this.router.navigate(['/projects']),
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
      error: () => {
        this.matchingLoading = false;
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

  /** Vérifie que l’utilisateur connecté est bien le propriétaire du projet. */
  get canBuildTeam(): boolean {
    if (!this.project || !this.isProjectOwner) return false;
    const uid = this.auth.getCurrentUser()?.id;
    return uid != null && Number(uid) === this.project.projectOwnerId;
  }

  /** Envoie les notifications aux membres de l’équipe (stockage + affichage côté freelancer). */
  getTeamMemberEmail(freelancerId: number): string | undefined {
    return this.teamResult?.notificationsToSend?.find((n) => n.freelancerId === freelancerId)?.email;
  }

  onSendNotifications(): void {
    if (!this.teamResult || !this.project) return;
    this.notificationService.sendBulk(
      this.project.id,
      this.project.title,
      this.teamResult.notificationsToSend
    );
    this.toast.success(`${this.teamResult.notificationsToSend.length} notification(s) envoyée(s). Les freelancers les verront dans leur espace.`);
  }

  /** Construit l’équipe optimale via l’IA et prépare les notifications. */
  onBuildTeam(): void {
    if (!this.project) return;
    this.buildTeamLoading = true;
    this.teamResult = null;
    this.teamAi.analyzeProject({ title: this.project.title, description: this.project.description }).subscribe({
      next: (analysis) => {
        const projectAnalysis: ProjectAnalysisForTeam = {
          complexity: analysis.complexity,
          roles: analysis.roles?.length ? analysis.roles : ['Développeur'],
          requiredSkills: analysis.requiredSkills ?? this.project!.requiredSkills ?? [],
          technicalLeaderRole: analysis.technicalLeaderRole ?? undefined,
        };
        this.userService.getAll('FREELANCER').subscribe({
          next: (users) => {
            this.skillService.getAll().subscribe({
              next: (allSkills) => {
                const skillsByFreelancer = new Map<number, string[]>();
                for (const s of allSkills) {
                  const list = skillsByFreelancer.get(s.freelancerId) ?? [];
                  list.push((s.name ?? '').trim());
                  skillsByFreelancer.set(s.freelancerId, list);
                }
                const freelancers: FreelancerForTeam[] = users.map((u) => {
                  const skills = skillsByFreelancer.get(u.id) ?? [];
                  const us = u as { rating?: number; completedProjects?: number };
                  return {
                    freelancerId: u.id,
                    fullName: u.fullName ?? u.username ?? u.email,
                    email: u.email,
                    skills,
                    yearsOfExperience: undefined,
                    rating: us.rating,
                    completedProjects: us.completedProjects,
                  };
                });
                this.teamAi
                  .buildTeam({
                    projectId: this.project!.id,
                    projectTitle: this.project!.title,
                    projectAnalysis,
                    freelancers,
                    maxTeamSize: 8,
                  })
                  .subscribe({
                    next: (res) => {
                      this.teamResult = res;
                      this.buildTeamLoading = false;
                    },
                    error: () => {
                      this.buildTeamLoading = false;
                      this.toast.error('Service Team AI indisponible. Lancez le service Python (port 5000).');
                    },
                  });
              },
              error: () => {
                this.buildTeamLoading = false;
              },
            });
          },
          error: () => {
            this.buildTeamLoading = false;
          },
        });
      },
      error: () => {
        this.buildTeamLoading = false;
        this.toast.error('Impossible d’analyser le projet. Vérifiez le service Team AI.');
      },
    });
  }
}
