import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SkillService } from '../../services/skill.service';
import { UserService } from '../../services/user.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { ConfirmService } from '../../services/confirm.service';
import { Skill, SkillRequest, SkillCategory, Portfolio } from '../../models/skill.model';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-freelancer-skills',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './freelancer-skills.component.html',
})
export class FreelancerSkillsComponent implements OnInit {
  /** ID du freelancer connecté = userId (chaque profil a son propre espace). */
  freelancerId = 0;
  currentUser: User | null = null;
  fullName = '';
  /** Photo de profil (base64), stockée en localStorage si pas d’API avatar. */
  avatarDataUrl: string | null = null;
  editingName = false;
  skills: Skill[] = [];
  cv?: any;
  portfolios: Portfolio[] = [];
  loading = true;
  saving = false;
  showPortfolioForm = false;
  editingPortfolioId: number | null = null;
  portfolioForm = { portfolioUrl: '', portfolioDescription: '' };
  bio = '';

  showSkillForm = false;
  editingSkillId: number | null = null;
  skillForm: SkillRequest = {
    name: '',
    category: SkillCategory.OTHER,
    freelancerId: 0,
    level: undefined,
    yearsOfExperience: undefined,
  };

  categories = Object.values(SkillCategory);
  categoryLabels: Record<SkillCategory, string> = {
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

  proficiencyLevels = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'];

  constructor(
    private skillService: SkillService,
    private userService: UserService,
    private toast: ToastService,
    private auth: AuthService,
    private router: Router,
    private confirmService: ConfirmService
  ) {}

  private get avatarStorageKey(): string {
    return `freelancer_avatar_${this.freelancerId}`;
  }

  ngOnInit() {
    const user = this.auth.getCurrentUser();
    const id = user?.id != null ? Number(user.id) : 0;
    if (!id) {
      this.toast.error('Session invalide. Veuillez vous reconnecter.');
      this.router.navigate(['/login']);
      return;
    }
    this.freelancerId = id;
    this.skillForm.freelancerId = id;
    const u = this.auth.getCurrentUser();
    this.fullName = (u?.fullName ?? u?.username ?? u?.email ?? 'Nom complet').toString().trim() || 'Nom complet';
    this.loadData();
  }

  /** Recharge uniquement la liste des portfolios (évite les races avec loadData) */
  loadPortfoliosOnly() {
    this.skillService.getPortfolios(this.freelancerId).subscribe({
      next: (list) => {
        this.portfolios = Array.isArray(list) ? list : (list != null && typeof list === 'object' && 'id' in list ? [list] : []);
      },
      error: (err) => {
        this.portfolios = [];
        this.toast.error(err?.error?.message || err?.message || 'Erreur chargement portfolios');
      },
    });
  }

  loadData() {
    this.loading = true;
    this.avatarDataUrl = localStorage.getItem(this.avatarStorageKey);
    this.userService.getById(this.freelancerId).subscribe({
      next: (user) => {
        this.currentUser = user;
        this.fullName = (user.fullName ?? user.username ?? user.email ?? '').toString().trim() || 'Nom complet';
      },
      error: () => {
        this.fullName = (this.auth.getCurrentUser()?.username ?? this.auth.getCurrentUser()?.email ?? 'Freelancer').toString();
      },
    });
    this.skillService.getByFreelancerId(this.freelancerId).subscribe({
      next: (skills) => {
        this.skills = skills ?? [];
        this.loading = false;
      },
      error: (err) => {
        const msg = err.error?.message ?? err.error?.error ?? err.message ?? 'Failed to load skills';
        this.toast.error(typeof msg === 'string' ? msg : 'Failed to load skills');
        this.loading = false;
      },
    });

    // Load CV if exists (404 = pas de CV, on laisse cv undefined)
    this.skillService.getCV(this.freelancerId).subscribe({
      next: (cv) => {
        this.cv = cv;
      },
      error: (err) => {
        if (err.status !== 404) {
          this.toast.error(err.error?.message || err.message || 'Erreur chargement CV');
        }
      },
    });

    // Load Portfolios (multiple)
    this.skillService.getPortfolios(this.freelancerId).subscribe({
      next: (list) => {
        if (Array.isArray(list)) {
          this.portfolios = list;
        } else if (list != null && typeof list === 'object' && 'id' in list) {
          this.portfolios = [list];
        } else {
          this.portfolios = [];
        }
      },
      error: (err) => {
        this.portfolios = [];
        if (err?.status !== 404) {
          this.toast.error(err?.error?.message || err?.message || 'Erreur chargement portfolios');
        }
      },
    });

    // Load Bio (professional summary)
    this.skillService.getBio(this.freelancerId).subscribe({
      next: (res) => { this.bio = res?.bio ?? ''; },
      error: () => { this.bio = ''; },
    });
  }

  openAddSkillForm() {
    this.editingSkillId = null;
    this.skillForm = {
      name: '',
      category: SkillCategory.OTHER,
      freelancerId: this.freelancerId,
      level: undefined,
      yearsOfExperience: undefined,
    };
    this.showSkillForm = true;
  }

  openEditSkillForm(skill: Skill) {
    this.editingSkillId = skill.id;
    this.skillForm = {
      name: skill.name,
      category: skill.category,
      freelancerId: this.freelancerId,
      level: skill.level ?? undefined,
      yearsOfExperience: skill.yearsOfExperience ?? undefined,
    };
    this.showSkillForm = true;
  }

  cancelSkillForm() {
    this.showSkillForm = false;
    this.editingSkillId = null;
  }

  addSkill() {
    if (!this.skillForm.name?.trim()) {
      this.toast.error('Skill name is required');
      return;
    }
    this.saving = true;
    this.skillForm.freelancerId = this.freelancerId;
    if (this.editingSkillId != null) {
      this.skillService.update(this.editingSkillId, this.skillForm).subscribe({
        next: () => {
          this.toast.success('Skill updated successfully');
          this.showSkillForm = false;
          this.editingSkillId = null;
          this.loadData();
          this.saving = false;
        },
        error: (err) => {
          const msg = err.error?.message ?? err.error?.error ?? err.message ?? 'Failed to update skill';
          this.toast.error(typeof msg === 'string' ? msg : 'Failed to update skill');
          this.saving = false;
        },
      });
    } else {
      this.skillService.create(this.skillForm).subscribe({
        next: () => {
          this.toast.success('Skill added successfully');
          this.showSkillForm = false;
          this.skillForm = {
            name: '',
            category: SkillCategory.OTHER,
            freelancerId: this.freelancerId,
            level: undefined,
            yearsOfExperience: undefined,
          };
          this.loadData();
          this.saving = false;
        },
        error: (err) => {
          const msg = err.error?.message ?? err.error?.error ?? err.message ?? 'Failed to add skill';
          this.toast.error(typeof msg === 'string' ? msg : 'Failed to add skill');
          this.saving = false;
        },
      });
    }
  }

  async deleteSkill(id: number) {
    const ok = await this.confirmService.confirm('Supprimer ce skill ?');
    if (!ok) return;
    this.skillService.delete(id).subscribe({
      next: () => {
        this.toast.success('Skill supprimé');
        this.loadData();
      },
      error: () => this.toast.error('Échec suppression skill'),
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    this.saving = true;
    this.skillService.uploadCV(this.freelancerId, file).subscribe({
      next: (cv) => {
        this.cv = cv ?? { fileName: file.name };
        this.toast.success('CV enregistré avec succès');
        this.saving = false;
        input.value = '';
      },
      error: (err) => {
        const msg = err.error?.message ?? err.error?.error ?? err.message ?? 'Échec envoi CV';
        this.toast.error(typeof msg === 'string' ? msg : 'Échec envoi CV');
        this.saving = false;
        input.value = '';
      },
    });
  }

  async deleteCV() {
    const ok = await this.confirmService.confirm('Supprimer votre CV ?');
    if (!ok) return;
    this.skillService.deleteCV(this.freelancerId).subscribe({
      next: () => {
        this.cv = undefined;
        this.toast.success('CV supprimé');
      },
      error: (err) => {
        const msg = err?.error?.message ?? err?.message ?? 'Impossible de supprimer le CV';
        this.toast.error(typeof msg === 'string' ? msg : 'Erreur suppression CV');
      },
    });
  }

  openAddPortfolioForm() {
    this.editingPortfolioId = null;
    this.portfolioForm = { portfolioUrl: '', portfolioDescription: '' };
    this.showPortfolioForm = true;
  }

  openEditPortfolioForm(p: Portfolio) {
    this.editingPortfolioId = p.id;
    this.portfolioForm = {
      portfolioUrl: p.portfolioUrl ?? '',
      portfolioDescription: p.portfolioDescription ?? '',
    };
    this.showPortfolioForm = true;
  }

  cancelPortfolioForm() {
    this.showPortfolioForm = false;
    this.editingPortfolioId = null;
  }

  /** Vérifie que la valeur est une URL valide (http ou https). Le portfolio n'accepte que des liens. */
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
    const url = this.portfolioForm.portfolioUrl?.trim();
    if (!url) {
      this.toast.error('L’URL du portfolio est obligatoire.');
      return;
    }
    if (!this.isValidPortfolioUrl(url)) {
      this.toast.error('Seuls les liens (URL) sont acceptés. Exemple : https://mon-portfolio.com');
      return;
    }
    this.saving = true;
    const desc = this.portfolioForm.portfolioDescription?.trim() || undefined;
    if (this.editingPortfolioId != null) {
      this.skillService.updatePortfolio(this.editingPortfolioId, url, desc).subscribe({
        next: () => {
          this.toast.success('Portfolio mis à jour');
          this.showPortfolioForm = false;
          this.editingPortfolioId = null;
          this.loadPortfoliosOnly();
          this.saving = false;
        },
        error: () => {
          this.toast.error('Failed to update portfolio');
          this.saving = false;
        },
      });
    } else {
      this.skillService.addPortfolio(this.freelancerId, url, desc).subscribe({
        next: () => {
          this.toast.success('Portfolio ajouté');
          this.showPortfolioForm = false;
          this.editingPortfolioId = null;
          this.portfolioForm = { portfolioUrl: '', portfolioDescription: '' };
          this.loadPortfoliosOnly();
          this.saving = false;
        },
        error: (err) => {
          const msg = err?.error?.message ?? err?.error?.error ?? err?.message ?? 'Échec ajout portfolio. Vérifiez que le service Skill (port 8083) est démarré.';
          this.toast.error(typeof msg === 'string' ? msg : 'Échec ajout portfolio');
          this.saving = false;
        },
      });
    }
  }

  async deletePortfolioItem(id: number) {
    const ok = await this.confirmService.confirm('Supprimer cette entrée portfolio ?');
    if (!ok) return;
    this.skillService.deletePortfolio(id).subscribe({
      next: () => {
        this.toast.success('Portfolio supprimé');
        this.loadPortfoliosOnly();
      },
      error: () => this.toast.error('Échec suppression portfolio'),
    });
  }

  saveBio() {
    this.saving = true;
    this.skillService.saveBio(this.freelancerId, this.bio).subscribe({
      next: () => {
        this.toast.success('Résumé enregistré');
        this.saving = false;
      },
      error: (err) => {
        const msg = err.error?.message ?? err.error?.error ?? err.message ?? 'Impossible d\'enregistrer le résumé. Vérifiez que le backend et la gateway sont démarrés.';
        this.toast.error(typeof msg === 'string' ? msg : 'Erreur enregistrement résumé');
        this.saving = false;
      },
    });
  }

  async clearBio() {
    const ok = await this.confirmService.confirm('Effacer le résumé professionnel ?');
    if (!ok) return;
    this.bio = '';
    this.saving = true;
    this.skillService.saveBio(this.freelancerId, '').subscribe({
      next: () => {
        this.toast.success('Summary cleared');
        this.saving = false;
      },
      error: () => {
        this.toast.error('Failed to clear');
        this.saving = false;
      },
    });
  }

  /** Supprimer la bio côté serveur (puis vider l’affichage) */
  toggleEditName(): void {
    this.editingName = !this.editingName;
    if (!this.editingName && this.fullName.trim()) this.saveFullName();
  }

  saveFullName(): void {
    const name = this.fullName.trim();
    if (!name) return;
    this.saving = true;
    this.userService.update(this.freelancerId, { fullName: name } as any).subscribe({
      next: () => {
        this.currentUser = this.currentUser ? { ...this.currentUser, fullName: name } : null;
        this.toast.success('Nom enregistré');
        this.editingName = false;
        this.saving = false;
      },
      error: (err) => {
        const msg = err?.error?.message ?? err?.message ?? 'Impossible d\'enregistrer le nom';
        this.toast.error(typeof msg === 'string' ? msg : 'Erreur');
        this.saving = false;
      },
    });
  }

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !file.type.startsWith('image/')) {
      this.toast.error('Choisissez une image (JPG, PNG, WebP).');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      if (dataUrl.length > 500_000) {
        this.toast.error('Image trop lourde. Utilisez une image plus petite.');
        return;
      }
      localStorage.setItem(this.avatarStorageKey, dataUrl);
      this.avatarDataUrl = dataUrl;
      this.toast.success('Photo de profil enregistrée');
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  removeAvatar(): void {
    localStorage.removeItem(this.avatarStorageKey);
    this.avatarDataUrl = null;
    this.toast.success('Photo supprimée');
  }

  async deleteBio() {
    const ok = await this.confirmService.confirm('Supprimer définitivement votre résumé professionnel ?');
    if (!ok) return;
    this.saving = true;
    this.skillService.deleteBio(this.freelancerId).subscribe({
      next: () => {
        this.bio = '';
        this.toast.success('Résumé supprimé');
        this.saving = false;
      },
      error: () => {
        this.toast.error('Échec de la suppression');
        this.saving = false;
      },
    });
  }
}
