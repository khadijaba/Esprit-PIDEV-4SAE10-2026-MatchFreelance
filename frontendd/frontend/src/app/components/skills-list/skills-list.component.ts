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
  categories = Object.values(SkillCategory);
  categoryLabels: Record<SkillCategory | 'ALL', string> = {
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
    ALL: 'All Categories'
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
    this.loadSkills();
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
