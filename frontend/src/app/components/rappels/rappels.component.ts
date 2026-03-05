import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

export interface RappelItem {
  type: string;
  message: string;
  detail?: string;
  examenTitre?: string;
  formationId?: number;
  examenId?: number;
}

export interface FreelancerRappels {
  freelancer_id: number;
  email: string;
  fullName: string;
  rappels: RappelItem[];
}

export interface RappelsData {
  generatedAt: string;
  summary: { freelancers: number; rappels_total: number };
  rappels: FreelancerRappels[];
}

const TYPE_LABELS: Record<string, string> = {
  formation_ouverte: 'Formations ouvertes',
  examen_a_passer: 'Examen à passer',
  examen_non_passe_retard: 'Examen en retard',
  formation_se_termine: 'Formation se termine',
  certificat: 'Certificats',
};

@Component({
  selector: 'app-rappels',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rappels.component.html',
})
export class RappelsComponent implements OnInit {
  data: RappelsData | null = null;
  loading = true;
  error: string | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.http.get<RappelsData>('/reports/rappels.json').subscribe({
      next: (d) => {
        this.data = d;
        this.loading = false;
      },
      error: () => {
        this.error =
          'Fichier non disponible. Dans le dossier python, exécutez : python rappels.py --export';
        this.loading = false;
      },
    });
  }

  typeLabel(type: string): string {
    return TYPE_LABELS[type] ?? type;
  }

  formatDate(iso: string): string {
    try {
      const d = new Date(iso);
      return d.toLocaleString('fr-FR', {
        dateStyle: 'short',
        timeStyle: 'short',
      });
    } catch {
      return iso;
    }
  }
}
