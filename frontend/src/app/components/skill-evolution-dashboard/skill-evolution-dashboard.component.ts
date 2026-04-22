import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SkillService } from '../../services/skill.service';
import { SkillEvolutionService } from '../../services/skill-evolution.service';
import { AuthService } from '../../services/auth.service';
import { Skill, SkillCategory } from '../../models/skill.model';
import { SkillEvolutionEntry, getLevelLabel, getLevelOrder, LEVEL_LABELS } from '../../models/skill-evolution.model';

/** Score 0–100 par compétence : niveau (1–4 → 25–100) + bonus expérience (max +15) */
const MAX_YEARS_FOR_BONUS = 10;

@Component({
  selector: 'app-skill-evolution-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './skill-evolution-dashboard.component.html',
})
export class SkillEvolutionDashboardComponent implements OnInit {
  freelancerId = 0;
  skills: Skill[] = [];
  evolutionHistory: SkillEvolutionEntry[] = [];
  loading = true;
  addedLast30 = 0;
  upgradedLast30 = 0;

  /** Score global profil 0–100 (moyenne niveaux + bonus expérience) */
  overallScore = 0;
  /** Répartition par niveau pour le diagramme en barres */
  levelDistribution: { label: string; count: number; order: number }[] = [];

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

  constructor(
    private skillService: SkillService,
    private evolutionService: SkillEvolutionService,
    private auth: AuthService
  ) {}

  ngOnInit() {
    const user = this.auth.getCurrentUser();
    const id = user?.id != null ? Number(user.id) : 0;
    if (!id) return;
    this.freelancerId = id;
    this.load();
  }

  load() {
    this.loading = true;
    this.skillService.getByFreelancerId(this.freelancerId).subscribe({
      next: (skills) => {
        this.skills = skills ?? [];
        this.evolutionHistory = this.evolutionService.getHistory(this.freelancerId);
        this.addedLast30 = this.evolutionService.getAddedCount(this.freelancerId, 30);
        this.upgradedLast30 = this.evolutionService.getUpgradedCount(this.freelancerId, 30);
        this.computeOverallScoreAndDistribution();
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  private computeOverallScoreAndDistribution(): void {
    const levels = ['JUNIOR', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'] as const;
    const orderByLevel: Record<string, number> = { JUNIOR: 1, BEGINNER: 1, INTERMEDIATE: 2, ADVANCED: 3, EXPERT: 4 };
    const counts: Record<string, number> = { JUNIOR: 0, INTERMEDIATE: 0, ADVANCED: 0, EXPERT: 0 };
    let levelSum = 0;
    let experienceBonus = 0;
    for (const s of this.skills) {
      const raw = (s.level || 'JUNIOR').toUpperCase();
      const lvl = raw === 'BEGINNER' ? 'JUNIOR' : raw;
      const order = orderByLevel[raw] ?? orderByLevel[lvl] ?? 1;
      counts[lvl] = (counts[lvl] ?? 0) + 1;
      levelSum += order;
      const years = s.yearsOfExperience ?? 0;
      experienceBonus += Math.min(1.5 * years, 15); // max +15 per skill, scaled by count later
    }
    const n = this.skills.length;
    if (n > 0) {
      const avgLevel = levelSum / n; // 1..4
      const levelScore = ((avgLevel - 1) / 3) * 100; // 0..100
      const expBonus = Math.min(30, (experienceBonus / n) * 2); // cap 30 bonus
      this.overallScore = Math.round(Math.min(100, levelScore + expBonus));
    } else {
      this.overallScore = 0;
    }
    this.levelDistribution = levels.map((key) => ({
      label: LEVEL_LABELS[key] ?? key,
      count: counts[key] ?? 0,
      order: orderByLevel[key] ?? 0,
    }));
  }

  /** Score 0–100 pour une compétence (heatmap / barre de progression) */
  skillScore(skill: Skill): number {
    const order = getLevelOrder(skill.level);
    const levelPct = order >= 1 ? ((order - 1) / 3) * 100 : 0;
    const years = skill.yearsOfExperience ?? 0;
    const expBonus = Math.min(15, (years / MAX_YEARS_FOR_BONUS) * 15);
    return Math.round(Math.min(100, levelPct + expBonus));
  }

  /** Pourcentage max des barres (répartition par niveau) */
  get levelMaxCount(): number {
    const m = Math.max(...this.levelDistribution.map((d) => d.count), 1);
    return m;
  }

  getLevelLabel(level: string | undefined): string {
    return getLevelLabel(level);
  }

  getLevelOrder(level: string | undefined): number {
    return getLevelOrder(level);
  }

  /** Couleur heatmap selon niveau (1=junior → 4=expert) */
  heatmapColor(level: string | undefined): string {
    const order = getLevelOrder(level);
    switch (order) {
      case 1: return 'bg-amber-200 text-amber-900';   // Junior
      case 2: return 'bg-sky-200 text-sky-900';       // Intermediate
      case 3: return 'bg-emerald-200 text-emerald-900'; // Advanced
      case 4: return 'bg-violet-200 text-violet-900'; // Expert
      default: return 'bg-gray-100 text-gray-600';
    }
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  }

  getCategoryLabel(cat: SkillCategory | string): string {
    return this.categoryLabels[cat as SkillCategory] ?? cat;
  }
}
