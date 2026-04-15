import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SkillService } from '../../services/skill.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { SkillRequest, SkillCategory, SKILL_CATEGORY_LABELS } from '../../models/skill.model';

@Component({
  selector: 'app-skill-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './skill-form.component.html',
})
export class SkillFormComponent implements OnInit {
  isEdit = false;
  skillId?: number;
  saving = false;
  error = '';

  form: SkillRequest = {
    name: '',
    category: 'WEB_DEVELOPMENT',
    freelancerId: 1,
    level: '',
    yearsOfExperience: undefined,
  };

  categories: { value: SkillCategory; label: string }[] = Object.entries(SKILL_CATEGORY_LABELS).map(
    ([value, label]) => ({ value: value as SkillCategory, label })
  );

  constructor(
    private skillService: SkillService,
    private auth: AuthService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const user = this.auth.getStoredUser();
    if (user?.role === 'FREELANCER' && user.userId) {
      this.form.freelancerId = user.userId;
    }
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.skillId = +id;
      this.skillService.getById(this.skillId).subscribe({
        next: (s) => {
          this.form = {
            name: s.name,
            category: s.category,
            freelancerId: s.freelancerId,
            level: s.level ?? '',
            yearsOfExperience: s.yearsOfExperience,
          };
        },
        error: () => this.router.navigate(['/admin/skills']),
      });
    }
  }

  onSubmit() {
    this.saving = true;
    this.error = '';
    const payload: SkillRequest = {
      ...this.form,
      level: this.form.level?.trim() || undefined,
      yearsOfExperience: this.form.yearsOfExperience ?? undefined,
    };
    if (this.isEdit && this.skillId) {
      this.skillService.update(this.skillId, payload).subscribe({
        next: () => {
          this.toast.success('Compétence mise à jour.');
          this.router.navigate(['/admin/skills']);
        },
        error: (err) => {
          this.saving = false;
          this.error = err?.error?.message ?? err?.message ?? 'Erreur lors de la mise à jour.';
        },
      });
    } else {
      this.skillService.create(payload).subscribe({
        next: () => {
          this.toast.success('Compétence créée.');
          this.router.navigate(['/admin/skills']);
        },
        error: (err) => {
          this.saving = false;
          this.error = err?.error?.message ?? err?.message ?? 'Erreur lors de la création.';
        },
      });
    }
  }
}
