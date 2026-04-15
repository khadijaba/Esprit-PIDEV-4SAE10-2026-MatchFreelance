import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';

export interface DropoutItem {
  freelancer_id: number;
  email: string | null;
  fullName: string | null;
  inscription_id: number;
  formation_id: number;
  formation_titre: string | null;
  jours_depuis_inscription: number;
  risk: string;
}

export interface ExamPrediction {
  freelancer_id: number;
  email: string | null;
  fullName: string | null;
  examen_id: number;
  examen_titre: string | null;
  formation_id: number;
  formation_titre: string | null;
  proba_reussite: number;
}

export interface ProjectReco {
  project_id: number;
  title: string | null;
  match_score: number;
  match_count: number;
}

export interface FormationReco {
  id: number;
  titre: string | null;
  typeFormation?: string | null;
  niveau?: string | null;
  statut?: string | null;
  dateDebut?: string | null;
  dateFin?: string | null;
}

export interface MLPredictionsData {
  dropouts: DropoutItem[];
  project_recommendations_by_freelancer: Record<string, ProjectReco[]>;
  formation_recommendations_by_freelancer?: Record<string, FormationReco[]>;
  exam_success_predictions: ExamPrediction[];
  meta?: {
    jours_decrochage?: number;
    seuil_reussite_examen?: number;
    project_recommendation_note?: string | null;
    projects_count?: number;
  };
}

@Component({
  selector: 'app-previsionnel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './previsionnel.component.html',
})
export class PrevisionnelComponent implements OnInit {
  data: MLPredictionsData | null = null;
  loading = true;
  error: string | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<MLPredictionsData>('/reports/ml_predictions.json').subscribe({
      next: (d) => {
        this.data = d;
        this.loading = false;
      },
      error: () => {
        this.error = 'Fichier non disponible. Lancez dans le dossier python : python ml_predictions.py';
        this.loading = false;
      },
    });
  }

  projectRecsEntries(): [string, ProjectReco[]][] {
    if (!this.data?.project_recommendations_by_freelancer) return [];
    return Object.entries(this.data.project_recommendations_by_freelancer);
  }

  /** Projets recommandés avec un score > 0 uniquement (masque les 0 %). */
  projectRecsWithScore(recs: ProjectReco[]): ProjectReco[] {
    return (recs || []).filter((r) => (r.match_score ?? 0) > 0);
  }

  formationRecsForFreelancer(freelancerId: string): FormationReco[] {
    if (!this.data?.formation_recommendations_by_freelancer) return [];
    return this.data.formation_recommendations_by_freelancer[freelancerId] ?? [];
  }
}
