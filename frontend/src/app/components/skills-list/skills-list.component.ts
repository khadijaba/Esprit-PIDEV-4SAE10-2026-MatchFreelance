import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SkillService } from '../../services/skill.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmService } from '../../services/confirm.service';
import { Skill, SkillCategory } from '../../models/skill.model';

@Component({
  selector: 'app-skills-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './skills-list.component.html',
})
export class SkillsListComponent implements OnInit {
  skills: Skill[] = [];
  loading = true;
  selectedCategory: SkillCategory | 'ALL' = 'ALL';
  currentPage = 1;
  pageSize = 10;
  categories = Object.values(SkillCategory);
  categoryLabels: Record<string, string> = {
    WEB_DEVELOPMENT: 'Développement Web',
    MOBILE_DEVELOPMENT: 'Développement Mobile',
    AI: 'Intelligence Artificielle',
    DATA_SCIENCE: 'Data Science',
    DEVOPS: 'DevOps',
    CYBERSECURITY: 'Cybersécurité',
    DESIGN: 'Design',
    OTHER: 'Autre',
    ALL: 'Toutes les catégories',
  };

  constructor(
    private skillService: SkillService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit() {
    this.loadSkills();
  }

  loadSkills() {
    this.loading = true;
    if (this.selectedCategory === 'ALL') {
      this.skillService.getAll().subscribe({
        next: (skills) => {
          this.skills = skills;
          this.loading = false;
        },
        error: () => {
          this.toast.error('Failed to load skills');
          this.loading = false;
        },
      });
    } else {
      this.skillService.getByCategory(this.selectedCategory).subscribe({
        next: (skills) => {
          this.skills = skills;
          this.loading = false;
        },
        error: () => {
          this.toast.error('Failed to load skills');
          this.loading = false;
        },
      });
    }
  }

  onCategoryChange() {
    this.currentPage = 1;
    this.loadSkills();
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.skills.length / this.pageSize));
  }

  get paginatedSkills(): Skill[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.skills.slice(start, start + this.pageSize);
  }

  goToPage(page: number) {
    this.currentPage = Math.max(1, Math.min(page, this.totalPages));
  }

  async deleteSkill(id: number) {
    const ok = await this.confirmService.confirm('Supprimer ce skill ?');
    if (!ok) return;
    this.skillService.delete(id).subscribe({
      next: () => {
        this.toast.success('Skill supprimé');
        this.loadSkills();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
    });
  }
}
