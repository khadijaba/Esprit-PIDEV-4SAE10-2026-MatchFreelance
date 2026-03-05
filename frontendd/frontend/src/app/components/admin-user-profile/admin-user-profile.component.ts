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

  categoryLabels: Record<string, string> = {
    WEB_DEVELOPMENT: 'Web Development',
    MOBILE_DEVELOPMENT: 'Mobile Development',
    DATA_SCIENCE: 'Data Science',
    DESIGN: 'Design',
    MARKETING: 'Marketing',
    WRITING: 'Writing',
    VIDEO_EDITING: 'Video Editing',
    PHOTOGRAPHY: 'Photography',
    CONSULTING: 'Consulting',
    OTHER: 'Other',
  };

  statusClass(status: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
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
        next: (s) => (this.skills = s ?? []),
        error: () => {},
      });
      this.skillService.getCV(this.userId).subscribe({
        next: (c) => (this.cv = c),
        error: () => {},
      });
      this.skillService.getPortfolios(this.userId).subscribe({
        next: (p) => (this.portfolios = Array.isArray(p) ? p : []),
        error: () => (this.portfolios = []),
      });
      this.skillService.getBio(this.userId).subscribe({
        next: (b) => (this.bio = b?.bio ?? ''),
        error: () => (this.bio = ''),
      });
    }

    if (role === 'CLIENT' || role === 'PROJECT_OWNER') {
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
      CLIENT: 'Project Owner',
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
    return r === 'CLIENT' || r === 'PROJECT_OWNER';
  }

  private reloadFreelancerData() {
    if (!this.user || !this.isFreelancer()) return;
    this.skillService.getByFreelancerId(this.userId).subscribe({ next: (s) => (this.skills = s ?? []) });
    this.skillService.getCV(this.userId).subscribe({ next: (c) => (this.cv = c), error: () => (this.cv = null) });
    this.skillService.getPortfolios(this.userId).subscribe({
      next: (p) => (this.portfolios = Array.isArray(p) ? p : []),
      error: () => (this.portfolios = []),
    });
    this.skillService.getBio(this.userId).subscribe({ next: (b) => (this.bio = b?.bio ?? ''), error: () => (this.bio = '') });
  }

  private reloadProjects() {
    if (!this.isProjectOwner()) return;
    this.projectService.getByOwnerId(this.userId).subscribe({ next: (p) => (this.projects = p ?? []) });
  }

  async deleteSkill(skill: Skill) {
    const ok = await this.confirmService.confirm(`Supprimer le skill « ${skill.name } » ?`);
    if (!ok) return;
    this.skillService.delete(skill.id).subscribe({
      next: () => {
        this.toast.success('Skill supprimé');
        this.reloadFreelancerData();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
    });
  }

  async deleteCV() {
    const ok = await this.confirmService.confirm('Supprimer le CV de ce profil ?');
    if (!ok) return;
    this.skillService.deleteCV(this.userId).subscribe({
      next: () => {
        this.toast.success('CV supprimé');
        this.cv = null;
        this.reloadFreelancerData();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
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
    this.saving = true;
    this.skillService.updatePortfolio(this.editingPortfolioId, url, this.editPortfolioDesc?.trim() || undefined).subscribe({
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

  async deletePortfolio(p: { id: number; portfolioUrl?: string }) {
    const ok = await this.confirmService.confirm('Supprimer ce portfolio ?');
    if (!ok) return;
    this.skillService.deletePortfolio(p.id).subscribe({
      next: () => {
        this.toast.success('Portfolio supprimé');
        if (this.editingPortfolioId === p.id) this.cancelEditPortfolio();
        this.reloadFreelancerData();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
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
    this.skillService.saveBio(this.userId, this.bioEditValue).subscribe({
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

  async deleteBio() {
    const ok = await this.confirmService.confirm('Supprimer le résumé professionnel ?');
    if (!ok) return;
    this.saving = true;
    this.skillService.deleteBio(this.userId).subscribe({
      next: () => {
        this.bio = '';
        this.editingBio = false;
        this.bioEditValue = '';
        this.toast.success('Résumé supprimé');
        this.saving = false;
      },
      error: () => {
        this.toast.error('Erreur lors de la suppression');
        this.saving = false;
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
