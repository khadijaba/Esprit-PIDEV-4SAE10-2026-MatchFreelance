import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { ToastService } from '../../services/toast.service';
import {
  Formation,
  NiveauFormation,
  NIVEAU_FORMATION_LABELS,
  StatutFormation,
  TypeFormation,
  TYPE_FORMATION_LABELS,
} from '../../models/formation.model';

@Component({
  selector: 'app-formation-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './formation-list.component.html',
})
export class FormationListComponent implements OnInit {
  formations: Formation[] = [];
  filtered: Formation[] = [];
  paginatedFormations: Formation[] = [];
  searchTerm = '';
  statutFilter: StatutFormation | '' = '';
  loading = true;
  readonly pageSize = 5;
  currentPage = 1;
  totalPages = 1;

  constructor(
    private formationService: FormationService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.formationService.getAll().subscribe({
      next: (data) => {
        this.formations = data;
        this.currentPage = 1;
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Impossible de charger les formations');
      },
    });
  }

  applyFilters() {
    let result = this.formations;
    if (this.statutFilter) {
      result = result.filter((f) => f.statut === this.statutFilter);
    }
    if (this.searchTerm.trim()) {
      const q = this.searchTerm.toLowerCase();
      result = result.filter(
        (f) =>
          f.titre.toLowerCase().includes(q) ||
          (f.description && f.description.toLowerCase().includes(q))
      );
    }
    this.filtered = result;
    this.totalPages = Math.max(1, Math.ceil(this.filtered.length / this.pageSize));
    this.currentPage = Math.min(this.currentPage, this.totalPages);
    this.updatePaginatedSlice();
  }

  private updatePaginatedSlice() {
    const start = (this.currentPage - 1) * this.pageSize;
    this.paginatedFormations = this.filtered.slice(start, start + this.pageSize);
  }

  goToPage(page: number) {
    this.currentPage = Math.max(1, Math.min(page, this.totalPages));
    this.updatePaginatedSlice();
  }

  onDelete(id: number) {
    if (!confirm('Supprimer cette formation ?')) return;
    this.formationService.delete(id).subscribe({
      next: () => {
        this.toast.success('Formation supprimée');
        this.load();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
    });
  }

  typeFormationLabel(t?: TypeFormation): string {
    return t ? (TYPE_FORMATION_LABELS[t] ?? t) : '—';
  }

  niveauLabel(n?: NiveauFormation | null): string {
    return n ? (NIVEAU_FORMATION_LABELS[n] ?? n) : '—';
  }

  statutClass(s: StatutFormation): string {
    const map: Record<StatutFormation, string> = {
      OUVERTE: 'bg-emerald-100 text-emerald-700',
      EN_COURS: 'bg-amber-100 text-amber-700',
      TERMINEE: 'bg-blue-100 text-blue-700',
      ANNULEE: 'bg-red-100 text-red-700',
    };
    return map[s] ?? 'bg-gray-100 text-gray-700';
  }
}
