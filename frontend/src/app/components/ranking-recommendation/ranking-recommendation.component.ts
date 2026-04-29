import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExamenService } from '../../services/examen.service';
import { AuthService } from '../../services/auth.service';
import { FreelancerProjectMatching, FreelancerRanking } from '../../models/examen.model';

@Component({
  selector: 'app-ranking-recommendation',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ranking-recommendation.component.html',
})
export class RankingRecommendationComponent implements OnInit {
  private readonly examenService = inject(ExamenService);
  private readonly auth = inject(AuthService);

  rankings: FreelancerRanking[] = [];
  topPerformers: FreelancerRanking[] = [];
  projectMatching: FreelancerProjectMatching | null = null;

  rankingLoading = false;
  topLoading = false;
  matchingLoading = false;
  error: string | null = null;

  topLimit = 5;
  matchingFreelancerId: number | null = null;
  matchingLimit = 5;

  ngOnInit(): void {
    const u = this.auth.getStoredUser();
    if (u?.role === 'FREELANCER' && u.userId != null && u.userId > 0) {
      this.matchingFreelancerId = u.userId;
    }
    this.loadRanking();
    this.loadTopPerformers();
  }

  loadRanking(): void {
    this.rankingLoading = true;
    this.error = null;
    this.examenService.getGlobalRanking().subscribe({
      next: (data) => {
        this.rankingLoading = false;
        this.rankings = data ?? [];
        if (this.matchingFreelancerId == null && this.rankings.length > 0) {
          this.matchingFreelancerId = this.rankings[0].freelancerId;
        }
      },
      error: (err) => {
        this.rankingLoading = false;
        this.error = err?.error?.message ?? `Impossible de charger le ranking (HTTP ${err?.status ?? ''}).`;
      },
    });
  }

  loadTopPerformers(): void {
    this.topLoading = true;
    this.error = null;
    this.examenService.getTopPerformers(this.topLimit).subscribe({
      next: (data) => {
        this.topLoading = false;
        this.topPerformers = data ?? [];
      },
      error: (err) => {
        this.topLoading = false;
        this.error = err?.error?.message ?? `Impossible de charger le top performers (HTTP ${err?.status ?? ''}).`;
      },
    });
  }

  loadProjectMatching(): void {
    if (!this.matchingFreelancerId || this.matchingFreelancerId <= 0) {
      this.error = 'Renseignez un freelancerId valide.';
      return;
    }
    this.matchingLoading = true;
    this.error = null;
    this.projectMatching = null;
    this.examenService.getProjectMatching(this.matchingFreelancerId, this.matchingLimit).subscribe({
      next: (data) => {
        this.matchingLoading = false;
        this.projectMatching = data;
      },
      error: (err) => {
        this.matchingLoading = false;
        this.error =
          err?.error?.message ?? `Impossible de charger le matching projet (HTTP ${err?.status ?? ''}).`;
      },
    });
  }

  asDate(v?: string | null): string {
    if (!v) return '—';
    return new Date(v).toLocaleString('fr-FR');
  }
}
