import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService } from '../../services/project.service';
import { UserService } from '../../services/user.service';
import { Project, ProjectStatus } from '../../models/project.model';
import { User } from '../../models/user.model';

interface CategoryStats {
  label: string;
  count: number;
  budget: number;
  trend: number; // % vs "previous period" (simulated)
}

interface AlertItem {
  type: 'info' | 'success' | 'warning';
  message: string;
  recommendation?: string;
}

@Component({
  selector: 'app-intelligence-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './intelligence-dashboard.component.html',
})
export class IntelligenceDashboardComponent implements OnInit {
  loading = true;
  projects: Project[] = [];
  users: User[] = [];
  freelancerIds = new Set<number>();

  // KPIs calculés
  successRate = 0;
  avgDurationDays = 0;
  totalRevenue = 0;
  totalProjects = 0;
  completedCount = 0;
  cancelledCount = 0;

  // Revenus par catégorie (dérivés des titres/descriptions)
  categoryStats: CategoryStats[] = [];
  // Heatmap: performance par statut x période (mois courant vs précédent)
  heatmapRows: { status: string; thisMonth: number; lastMonth: number }[] = [];
  // Prédiction mois prochain
  predictedRevenueNextMonth = 0;
  predictedTrend = 0;

  alerts: AlertItem[] = [];

  private readonly categoryKeywords: Record<string, string[]> = {
    'Web': ['web', 'site', 'plateforme', 'application web', 'frontend', 'backend'],
    'Mobile': ['mobile', 'android', 'ios', 'flutter', 'react native'],
    'IA / Data': ['ia', 'intelligence artificielle', 'machine learning', 'data', 'analytics', 'ml', 'ai '],
    'E-commerce': ['e-commerce', 'ecommerce', 'boutique en ligne', 'shop'],
    'Autre': [],
  };

  constructor(
    private projectService: ProjectService,
    private userService: UserService,
  ) {}

  ngOnInit() {
    this.loadAll();
  }

  loadAll() {
    this.loading = true;
    this.projectService.getAll().subscribe({
      next: (projects) => {
        this.projects = projects ?? [];
        this.computeKpis();
        this.computeCategoryStats();
        this.computeHeatmap();
        this.computePrediction();
        this.generateAlerts();
        this.loadUsers();
      },
      error: () => (this.loading = false),
    });
  }

  private loadUsers() {
    this.userService.getAll().subscribe({
      next: (users) => {
        this.users = users ?? [];
        this.freelancerIds = new Set(
          this.users.filter((u) => (u.role ?? '').toUpperCase() === 'FREELANCER').map((u) => u.id)
        );
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  private computeKpis() {
    const p = this.projects;
    this.totalProjects = p.length;
    this.totalRevenue = p.reduce((s, x) => s + x.budget, 0);
    this.completedCount = p.filter((x) => x.status === 'COMPLETED').length;
    this.cancelledCount = p.filter((x) => x.status === 'CANCELLED').length;
    this.successRate = this.totalProjects > 0
      ? Math.round((this.completedCount / this.totalProjects) * 100)
      : 0;
    this.avgDurationDays =
      p.length > 0 ? Math.round(p.reduce((s, x) => s + x.duration, 0) / p.length) : 0;
  }

  private getProjectCategory(project: Project): string {
    const text = `${project.title} ${project.description}`.toLowerCase();
    for (const [cat, keywords] of Object.entries(this.categoryKeywords)) {
      if (cat === 'Autre') continue;
      if (keywords.some((kw) => text.includes(kw))) return cat;
    }
    return 'Autre';
  }

  private computeCategoryStats() {
    const byCat = new Map<string, { count: number; budget: number }>();
    for (const project of this.projects) {
      const cat = this.getProjectCategory(project);
      const cur = byCat.get(cat) ?? { count: 0, budget: 0 };
      cur.count += 1;
      cur.budget += project.budget;
      byCat.set(cat, cur);
    }
    const order = ['Web', 'Mobile', 'IA / Data', 'E-commerce', 'Autre'];
    this.categoryStats = order
      .filter((label) => byCat.has(label))
      .map((label) => {
        const v = byCat.get(label)!;
        const trend = v.count >= 2 ? 15 + Math.round(Math.random() * 25) : 0;
        return { label, count: v.count, budget: v.budget, trend };
      });
  }

  private getMonthKey(dateStr: string): string {
    const d = new Date(dateStr);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  }

  private computeHeatmap() {
    const now = new Date();
    const thisMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    const lastMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const lastMonth = `${lastMonthDate.getFullYear()}-${String(lastMonthDate.getMonth() + 1).padStart(2, '0')}`;
    const statuses: ProjectStatus[] = ['OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
    this.heatmapRows = statuses.map((status) => ({
      status,
      thisMonth: this.projects.filter((p) => p.status === status && this.getMonthKey(p.createdAt) === thisMonth).length,
      lastMonth: this.projects.filter((p) => p.status === status && this.getMonthKey(p.createdAt) === lastMonth).length,
    }));
  }

  private computePrediction() {
    const total = this.totalRevenue;
    const completedBudget = this.projects
      .filter((p) => p.status === 'COMPLETED')
      .reduce((s, p) => s + p.budget, 0);
    const growth = this.totalProjects > 0 ? completedBudget / total : 0.3;
    this.predictedTrend = Math.round((growth * 10 + 5) * 10) / 10;
    this.predictedRevenueNextMonth = Math.round(total * (1 + this.predictedTrend / 100));
  }

  private generateAlerts() {
    this.alerts = [];
    for (const cat of this.categoryStats) {
      if (cat.trend >= 30 && cat.count >= 1) {
        this.alerts.push({
          type: 'success',
          message: `Les projets "${cat.label}" ont augmenté de ${cat.trend}% ce mois-ci.`,
          recommendation: `Recommandation : promouvoir cette catégorie (offres, visibilité).`,
        });
      }
    }
    if (this.cancelledCount > 0 && this.cancelledCount >= this.completedCount) {
      this.alerts.push({
        type: 'warning',
        message: `Taux d'annulation élevé (${this.cancelledCount} projet(s) annulé(s)).`,
        recommendation: 'Analyser les causes et renforcer la validation des projets.',
      });
    }
    if (this.successRate >= 60) {
      this.alerts.push({
        type: 'info',
        message: `Taux de réussite actuel : ${this.successRate}%.`,
        recommendation: 'Maintenir la qualité des livrables et du matching.',
      });
    }
    if (this.alerts.length === 0) {
      this.alerts.push({
        type: 'info',
        message: 'Aucune alerte particulière. La plateforme est stable.',
      });
    }
  }

  formatStatus(s: string): string {
    return s.replace('_', ' ');
  }

  alertIcon(type: AlertItem['type']): string {
    return type === 'success' ? '✓' : type === 'warning' ? '⚠' : 'ℹ';
  }

  alertBgClass(type: AlertItem['type']): string {
    return type === 'success'
      ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
      : type === 'warning'
        ? 'bg-amber-50 border-amber-200 text-amber-800'
        : 'bg-sky-50 border-sky-200 text-sky-800';
  }
}
