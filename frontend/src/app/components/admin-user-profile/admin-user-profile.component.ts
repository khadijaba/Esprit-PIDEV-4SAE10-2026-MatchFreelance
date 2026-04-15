import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';
import { SkillService } from '../../services/skill.service';
import { ProjectService } from '../../services/project.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmService } from '../../services/confirm.service';
import { User } from '../../models/user.model';
import { Skill } from '../../models/skill.model';
import { Project, ProjectStatus } from '../../models/project.model';

@Component({
  selector: 'app-admin-user-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-user-profile.component.html',
})
export class AdminUserProfileComponent implements OnInit {
  user: User | null = null;
  userId = 0;
  loading = true;
  notFound = false;
  saving = false;

  skills: Skill[] = [];
  cv: any = null;
  portfolios: any[] = [];
  bio = '';
  projects: Project[] = [];

  editingPortfolioId: number | null = null;
  editPortfolioUrl = '';
  editPortfolioDesc = '';
  editingBio = false;
  bioEditValue = '';
  /** État bloqué du résumé (supervision), fusionné avec localStorage si besoin */
  bioBlocked = false;

  categoryLabels: Record<string, string> = {
    WEB_DEVELOPMENT: 'Développement Web',
    MOBILE_DEVELOPMENT: 'Développement Mobile',
    AI: 'Intelligence Artificielle',
    DATA_SCIENCE: 'Data Science',
    DEVOPS: 'DevOps',
    CYBERSECURITY: 'Cybersécurité',
    DESIGN: 'Design',
    OTHER: 'Autre',
  };

  statusClass(status: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      DRAFT: 'bg-slate-200 text-slate-800',
      OPEN: 'bg-emerald-100 text-emerald-700',
      IN_PROGRESS: 'bg-amber-100 text-amber-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-700',
    };
    return map[status] ?? 'bg-gray-100 text-gray-700';
  }

  constructor(
    private route: ActivatedRoute,
    private userService: UserService,
    private skillService: SkillService,
    private projectService: ProjectService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    this.userId = id ? +id : 0;
    if (!this.userId) {
      this.notFound = true;
      this.loading = false;
      return;
    }
    this.loadUser();
  }

  loadUser() {
    this.loading = true;
    this.userService.getAll().subscribe({
      next: (users) => {
        this.user = users?.find((u) => Number(u.id) === this.userId) ?? null;
        if (!this.user) {
          this.notFound = true;
          this.loading = false;
          return;
        }
        this.loadSupervisionData();
      },
      error: () => {
        this.notFound = true;
        this.loading = false;
        this.toast.error('Impossible de charger le profil');
      },
    });
  }

  loadSupervisionData() {
    const role = (this.user?.role ?? '').toUpperCase();

    if (role === 'FREELANCER') {
      this.skillService.getByFreelancerId(this.userId).subscribe({
        next: (s) => (this.skills = this.skillService.mergeBlockedState(s ?? [])),
        error: () => {},
      });
      this.skillService.getCV(this.userId).subscribe({
        next: (c) => {
          this.cv = c;
          if (this.cv) this.cv.blocked = this.skillService.isCVBlocked(this.cv, this.userId);
        },
        error: () => {},
      });
      this.skillService.getPortfolios(this.userId).subscribe({
        next: (p) => (this.portfolios = this.skillService.mergePortfolioBlocked(Array.isArray(p) ? p : [])),
        error: () => (this.portfolios = []),
      });
      this.skillService.getBio(this.userId).subscribe({
        next: (b) => {
          this.bio = b?.bio ?? '';
          this.bioBlocked = this.skillService.isBioBlocked(this.bio, this.userId);
        },
        error: () => (this.bio = ''),
      });
    }

    if (role === 'PROJECT_OWNER') {
      this.projectService.getByOwnerId(this.userId).subscribe({
        next: (p) => (this.projects = p ?? []),
        error: () => (this.projects = []),
      });
    }

    this.loading = false;
  }

  roleLabel(role: string | undefined): string {
    if (!role) return '—';
    const map: Record<string, string> = {
      FREELANCER: 'Freelancer',
      PROJECT_OWNER: 'Project Owner',
      ADMIN: 'Admin',
    };
    return map[role.toUpperCase()] ?? role;
  }

  displayName(): string {
    if (!this.user) return '';
    return (
      (this.user.fullName ?? this.user.username ?? this.user.email ?? `#${this.user.id}`).toString().trim() ||
      `Profil #${this.user.id}`
    );
  }

  isFreelancer(): boolean {
    const r = (this.user?.role ?? '').toUpperCase();
    return r === 'FREELANCER';
  }

  isProjectOwner(): boolean {
    const r = (this.user?.role ?? '').toUpperCase();
    return r === 'PROJECT_OWNER';
  }

  private reloadFreelancerData() {
    if (!this.user || !this.isFreelancer()) return;
    this.skillService.getByFreelancerId(this.userId).subscribe({ next: (s) => (this.skills = this.skillService.mergeBlockedState(s ?? [])) });
    this.skillService.getCV(this.userId).subscribe({
      next: (c) => {
        this.cv = c;
        if (this.cv) this.cv.blocked = this.skillService.isCVBlocked(this.cv, this.userId);
      },
      error: () => (this.cv = null),
    });
    this.skillService.getPortfolios(this.userId).subscribe({
      next: (p) => (this.portfolios = this.skillService.mergePortfolioBlocked(Array.isArray(p) ? p : [])),
      error: () => (this.portfolios = []),
    });
    this.skillService.getBio(this.userId).subscribe({
      next: (b) => {
        this.bio = b?.bio ?? '';
        this.bioBlocked = this.skillService.isBioBlocked(this.bio, this.userId);
      },
      error: () => (this.bio = ''),
    });
  }

  private reloadProjects() {
    if (!this.isProjectOwner()) return;
    this.projectService.getByOwnerId(this.userId).subscribe({ next: (p) => (this.projects = p ?? []) });
  }

  /** Supervision : bloquer ou débloquer une compétence (pas de suppression). Fallback localStorage si le backend ne gère pas encore "blocked". */
  toggleBlockSkill(skill: Skill) {
    const blocked = !skill.blocked;
    this.skillService.setBlocked(skill, blocked).subscribe({
      next: (updated) => {
        skill.blocked = updated.blocked ?? blocked;
        this.skillService.setBlockedSkillIdLocal(skill.id, false); // synchronisé backend, plus besoin du local
        this.toast.success(blocked ? 'Compétence bloquée' : 'Compétence débloquée');
      },
      error: () => {
        this.skillService.setBlockedSkillIdLocal(skill.id, blocked);
        skill.blocked = blocked;
        this.toast.success(blocked ? 'Compétence bloquée (local)' : 'Compétence débloquée (local)');
      },
    });
  }

  /** Supervision : bloquer/débloquer le CV (pas de suppression). */
  toggleBlockCV() {
    if (!this.cv) return;
    const blocked = !this.skillService.isCVBlocked(this.cv, this.userId);
    this.skillService.setCVBlocked(this.userId, blocked).subscribe({
      next: (res) => {
        this.cv.blocked = res?.blocked ?? blocked;
        this.skillService.setCVBlockedLocal(this.userId, false);
        this.toast.success(blocked ? 'CV bloqué' : 'CV débloqué');
      },
      error: () => {
        this.skillService.setCVBlockedLocal(this.userId, blocked);
        this.cv.blocked = blocked;
        this.toast.success(blocked ? 'CV bloqué (local)' : 'CV débloqué (local)');
      },
    });
  }

  openEditPortfolio(p: { id: number; portfolioUrl?: string; portfolioDescription?: string }) {
    this.editingPortfolioId = p.id;
    this.editPortfolioUrl = p.portfolioUrl ?? '';
    this.editPortfolioDesc = p.portfolioDescription ?? '';
  }

  cancelEditPortfolio() {
    this.editingPortfolioId = null;
    this.editPortfolioUrl = '';
    this.editPortfolioDesc = '';
  }

  /** Le portfolio n'accepte que des liens (URL http/https). */
  private isValidPortfolioUrl(url: string): boolean {
    const trimmed = url?.trim();
    if (!trimmed) return false;
    if (!/^https?:\/\//i.test(trimmed)) return false;
    try {
      new URL(trimmed);
      return true;
    } catch {
      return false;
    }
  }

  savePortfolio() {
    const url = this.editPortfolioUrl?.trim();
    if (!url || this.editingPortfolioId == null) return;
    if (!this.isValidPortfolioUrl(url)) {
      this.toast.error('Seuls les liens (URL) sont acceptés. Exemple : https://mon-portfolio.com');
      return;
    }
    const portfolio = this.portfolios.find((p) => p.id === this.editingPortfolioId);
    const blocked = portfolio?.blocked;
    this.saving = true;
    this.skillService.updatePortfolio(this.editingPortfolioId, url, this.editPortfolioDesc?.trim() || undefined, blocked).subscribe({
      next: () => {
        this.toast.success('Portfolio mis à jour');
        this.cancelEditPortfolio();
        this.saving = false;
        this.reloadFreelancerData();
      },
      error: () => {
        this.toast.error('Erreur lors de la mise à jour');
        this.saving = false;
      },
    });
  }

  /** Supervision : bloquer/débloquer un portfolio (pas de suppression). */
  toggleBlockPortfolio(p: { id: number; portfolioUrl?: string; portfolioDescription?: string; blocked?: boolean }) {
    const blocked = !p.blocked;
    this.skillService.setPortfolioBlocked(p, blocked).subscribe({
      next: (updated) => {
        p.blocked = updated?.blocked ?? blocked;
        this.skillService.setPortfolioBlockedLocal(p.id, false);
        this.toast.success(blocked ? 'Portfolio bloqué' : 'Portfolio débloqué');
      },
      error: () => {
        this.skillService.setPortfolioBlockedLocal(p.id, blocked);
        p.blocked = blocked;
        this.toast.success(blocked ? 'Portfolio bloqué (local)' : 'Portfolio débloqué (local)');
      },
    });
  }

  startEditBio() {
    this.bioEditValue = this.bio;
    this.editingBio = true;
  }

  cancelEditBio() {
    this.editingBio = false;
    this.bioEditValue = '';
  }

  saveBio() {
    this.saving = true;
    this.skillService.saveBio(this.userId, this.bioEditValue, this.bioBlocked).subscribe({
      next: () => {
        this.bio = this.bioEditValue;
        this.toast.success('Résumé enregistré');
        this.editingBio = false;
        this.saving = false;
      },
      error: () => {
        this.toast.error('Erreur lors de l\'enregistrement');
        this.saving = false;
      },
    });
  }

  /** Supervision : bloquer/débloquer le résumé (pas de suppression). */
  toggleBlockBio() {
    const blocked = !this.bioBlocked;
    this.skillService.setBioBlocked(this.userId, blocked, this.bio || '').subscribe({
      next: () => {
        this.bioBlocked = blocked;
        this.skillService.setBioBlockedLocal(this.userId, false);
        this.toast.success(blocked ? 'Résumé bloqué' : 'Résumé débloqué');
      },
      error: () => {
        this.skillService.setBioBlockedLocal(this.userId, blocked);
        this.bioBlocked = blocked;
        this.toast.success(blocked ? 'Résumé bloqué (local)' : 'Résumé débloqué (local)');
      },
    });
  }

  async deleteProject(p: Project) {
    const ok = await this.confirmService.confirm(`Supprimer le projet « ${p.title } » ?`);
    if (!ok) return;
    this.projectService.delete(p.id).subscribe({
      next: () => {
        this.toast.success('Projet supprimé');
        this.reloadProjects();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
    });
  }
}
