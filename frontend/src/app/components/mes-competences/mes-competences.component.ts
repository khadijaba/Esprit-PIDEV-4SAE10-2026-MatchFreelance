import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SkillService } from '../../services/skill.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { Skill, SkillRequest, SkillCategory, SKILL_CATEGORY_LABELS } from '../../models/skill.model';

@Component({
  selector: 'app-mes-competences',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './mes-competences.component.html',
})
export class MesCompetencesComponent implements OnInit {
  skills: Skill[] = [];
  loading = true;
  error: string | null = null;
  showForm = false;
  editingId: number | null = null;
  saving = false;
  formError = '';

  form: SkillRequest = {
    name: '',
    category: 'WEB_DEVELOPMENT',
    freelancerId: 0,
    level: '',
    yearsOfExperience: undefined,
  };

  categories: { value: SkillCategory; label: string }[] = Object.entries(SKILL_CATEGORY_LABELS).map(
    ([value, label]) => ({ value: value as SkillCategory, label })
  );

  constructor(
    private skillService: SkillService,
    public auth: AuthService,
    private toast: ToastService
  ) {}

  get freelancerId(): number | null {
    const user = this.auth.getStoredUser();
    return user?.userId ?? null;
  }

  get isFreelancer(): boolean {
    return this.auth.getStoredUser()?.role === 'FREELANCER';
  }

  ngOnInit() {
    if (!this.auth.isLoggedIn()) {
      this.error = 'Connectez-vous pour gérer vos compétences.';
      this.loading = false;
      return;
    }
    if (!this.isFreelancer) {
      this.error = 'Seuls les freelancers peuvent gérer leurs compétences ici. Utilisez l’espace admin si vous êtes administrateur.';
      this.loading = false;
      return;
    }
    const id = this.freelancerId;
    if (!id) {
      this.error = 'Profil incomplet. Reconnectez-vous.';
      this.loading = false;
      return;
    }
    this.load(id);
  }

  load(freelancerId: number) {
    this.loading = true;
    this.skillService.getByFreelancer(freelancerId).subscribe({
      next: (data) => {
        this.skills = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Impossible de charger vos compétences. Le microservice Skill est-il démarré (port 8086) ?');
      },
    });
  }

  categoryLabel(cat: SkillCategory): string {
    return SKILL_CATEGORY_LABELS[cat] ?? cat;
  }

  openAddForm() {
    this.editingId = null;
    this.form = {
      name: '',
      category: 'WEB_DEVELOPMENT',
      freelancerId: this.freelancerId!,
      level: '',
      yearsOfExperience: undefined,
    };
    this.formError = '';
    this.showForm = true;
  }

  openEditForm(skill: Skill) {
    this.editingId = skill.id;
    this.form = {
      name: skill.name,
      category: skill.category,
      freelancerId: skill.freelancerId,
      level: skill.level ?? '',
      yearsOfExperience: skill.yearsOfExperience,
    };
    this.formError = '';
    this.showForm = true;
  }

  cancelForm() {
    this.showForm = false;
    this.editingId = null;
    this.formError = '';
  }

  onSubmit() {
    const id = this.freelancerId;
    if (!id) return;
    this.form.freelancerId = id;
    this.formError = '';
    this.saving = true;

    const payload: SkillRequest = {
      ...this.form,
      level: this.form.level?.trim() || undefined,
      yearsOfExperience: this.form.yearsOfExperience ?? undefined,
    };

    if (this.editingId !== null) {
      this.skillService.update(this.editingId, payload).subscribe({
        next: () => {
          this.toast.success('Compétence mise à jour.');
          this.cancelForm();
          this.load(id);
          this.saving = false;
        },
        error: (err) => {
          this.saving = false;
          this.formError = err?.error?.message ?? err?.message ?? 'Erreur lors de la mise à jour.';
        },
      });
    } else {
      this.skillService.create(payload).subscribe({
        next: () => {
          this.toast.success('Compétence ajoutée.');
          this.cancelForm();
          this.load(id);
          this.saving = false;
        },
        error: (err) => {
          this.saving = false;
          this.formError = err?.error?.message ?? err?.message ?? 'Erreur lors de l’ajout.';
        },
      });
    }
  }

  onDelete(skill: Skill) {
    if (!confirm(`Supprimer la compétence « ${skill.name } » ?`)) return;
    const id = this.freelancerId;
    if (!id) return;
    this.skillService.delete(skill.id).subscribe({
      next: () => {
        this.toast.success('Compétence supprimée.');
        this.load(id);
      },
      error: () => this.toast.error('Erreur lors de la suppression.'),
    });
  }
}
