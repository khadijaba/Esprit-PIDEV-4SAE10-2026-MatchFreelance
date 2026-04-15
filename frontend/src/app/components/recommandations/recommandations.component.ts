import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';

export interface ScoreCompletion {
  freelancer_id: number;
  email: string | null;
  fullName: string | null;
  score_completion_pct: number;
  nb_certificats: number;
  nb_formations_validees: number;
}

export interface FormationRecommendation {
  formation_id: number;
  titre: string | null;
  typeFormation: string;
  match_competence: boolean;
}

export interface FreelancerRecommendations {
  freelancer_id: number;
  email: string | null;
  recommendations: FormationRecommendation[];
}

export interface ParcoursStep {
  type: string;
  formation_id?: number;
  formation_titre?: string | null;
  examen_id?: number;
  examen_requis_id?: number;
  examen_titre?: string | null;
  examen_requis_titre?: string | null;
}

export interface RecommendationsData {
  scores_completion: ScoreCompletion[];
  recommendations_by_freelancer: FreelancerRecommendations[];
  parcours_prerequis: Record<string, ParcoursStep[]>;
}

@Component({
  selector: 'app-recommandations',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './recommandations.component.html',
})
export class RecommandationsComponent implements OnInit {
  data: RecommendationsData | null = null;
  loading = true;
  error: string | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<RecommendationsData>('/reports/recommendations.json').subscribe({
      next: (d) => {
        this.data = d;
        this.loading = false;
      },
      error: () => {
        this.error = 'Fichier non disponible. Lancez dans le dossier python : python recommendations.py';
        this.loading = false;
      },
    });
  }

  parcoursEntries(): [string, ParcoursStep[]][] {
    if (!this.data?.parcours_prerequis) return [];
    return Object.entries(this.data.parcours_prerequis);
  }
}
