import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { Formation, StatutFormation, TypeFormation, TYPE_FORMATION_LABELS } from '../../models/formation.model';

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

  constructor(private formationService: FormationService) {}

  ngOnInit() {
    this.formationService.getOuvertes().subscribe({
      next: (data) => {
        this.formations = data;
        this.applyFilters();
        this.loading = false;
      },
      error: () => (this.loading = false),
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
