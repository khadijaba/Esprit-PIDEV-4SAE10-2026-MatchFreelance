import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { SkillService } from '../../services/skill.service';
import { ToastService } from '../../services/toast.service';
import { Skill, SkillCategory, SKILL_CATEGORY_LABELS } from '../../models/skill.model';

@Component({
  selector: 'app-skill-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './skill-list.component.html',
})
export class SkillListComponent implements OnInit {
  skills: Skill[] = [];
  filtered: Skill[] = [];
  searchTerm = '';
  categoryFilter: SkillCategory | '' = '';
  freelancerFilter = '';
  loading = true;

  categories: { value: SkillCategory; label: string }[] = Object.entries(SKILL_CATEGORY_LABELS).map(
    ([value, label]) => ({ value: value as SkillCategory, label })
  );

  constructor(
    private skillService: SkillService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.skillService.getAll().subscribe({
      next: (data) => {
        this.skills = data;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Impossible de charger les compétences. Démarrez Eureka, la Gateway et le microservice Skill (port 8086).');
      },
    });
  }

  applyFilters() {
    let result = this.skills;
    if (this.categoryFilter) {
      result = result.filter((s) => s.category === this.categoryFilter);
    }
    if (this.freelancerFilter.trim()) {
      const id = this.freelancerFilter.trim();
      result = result.filter((s) => String(s.freelancerId) === id);
    }
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      result = result.filter(
        (s) =>
          s.name.toLowerCase().includes(q) ||
          (s.level && s.level.toLowerCase().includes(q))
      );
    }
    this.filtered = result;
  }

  categoryLabel(cat: SkillCategory | string): string {
    return SKILL_CATEGORY_LABELS[cat] ?? cat;
  }

  onDelete(id: number) {
    if (!confirm('Supprimer cette compétence ?')) return;
    this.skillService.delete(id).subscribe({
      next: () => {
        this.toast.success('Compétence supprimée.');
        this.load();
      },
      error: () => this.toast.error('Erreur lors de la suppression.'),
    });
  }
}
