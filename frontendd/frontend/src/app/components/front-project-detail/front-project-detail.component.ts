import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { TalentMatchingService } from '../../services/talent-matching.service';
import { SkillGapService } from '../../services/skill-gap.service';
import { SkillService } from '../../services/skill.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { DiscussionNavigationService } from '../../services/discussion-navigation.service';
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

  constructor(
    private projectService: ProjectService,
    private matchingService: TalentMatchingService,
    private skillGapService: SkillGapService,
    private skillService: SkillService,
    private auth: AuthService,
    private toast: ToastService,
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
}
