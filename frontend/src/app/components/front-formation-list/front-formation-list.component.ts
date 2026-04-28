import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { Formation, StatutFormation, TypeFormation, TYPE_FORMATION_LABELS } from '../../models/formation.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-front-formation-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './front-formation-list.component.html',
})
export class FrontFormationListComponent implements OnInit {
  formations: Formation[] = [];
  filtered: Formation[] = [];
  searchTerm = '';
  loading = true;
  showingAllFallback = false;

  constructor(
    private formationService: FormationService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.formationService.getOuvertes().subscribe({
      next: (data) => {
        const ouvertes = data ?? [];
        if (ouvertes.length > 0) {
          this.formations = ouvertes;
          this.showingAllFallback = false;
          this.applyFilters();
          this.loading = false;
          return;
        }
        this.formationService.getAll().subscribe({
          next: (all) => {
            this.formations = all ?? [];
            this.showingAllFallback = true;
            this.applyFilters();
            this.loading = false;
            if (this.formations.length > 0) {
              this.toast.info('Aucune formation ouverte actuellement : affichage de tout le catalogue.');
            }
          },
          error: () => {
            this.loading = false;
            this.toast.error('Impossible de charger les formations.');
          },
        });
      },
      error: () => {
        this.formationService.getAll().subscribe({
          next: (all) => {
            this.formations = all ?? [];
            this.showingAllFallback = true;
            this.applyFilters();
            this.loading = false;
            if (this.formations.length > 0) {
              this.toast.info('Service "formations ouvertes" indisponible : affichage du catalogue complet.');
            } else {
              this.toast.error('Impossible de charger les formations.');
            }
          },
          error: () => {
            this.loading = false;
            this.toast.error('Impossible de charger les formations.');
          },
        });
      },
    });
  }

  applyFilters() {
    let result = this.formations;
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      result = result.filter(
        (f) =>
          f.titre.toLowerCase().includes(q) ||
          (f.description && f.description.toLowerCase().includes(q))
      );
    }
    this.filtered = result;
  }

  typeFormationLabel(t?: TypeFormation): string {
    return t ? (TYPE_FORMATION_LABELS[t] ?? t) : '';
  }

  statutClass(s: StatutFormation): string {
    const map: Record<StatutFormation, string> = {
      OUVERTE: 'bg-emerald-100 text-emerald-700',
      EN_COURS: 'bg-amber-100 text-amber-700',
      TERMINEE: 'bg-blue-100 text-blue-700',
      ANNULEE: 'bg-red-100 text-red-700',
    };
    return map[s] ?? 'bg-slate-100 text-slate-600';
  }
}
