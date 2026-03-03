import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmService } from '../../services/confirm.service';
import { Formation, StatutFormation, TypeFormation, TYPE_FORMATION_LABELS } from '../../models/formation.model';

@Component({
  selector: 'app-formation-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './formation-list.component.html',
})
export class FormationListComponent implements OnInit {
  formations: Formation[] = [];
  filtered: Formation[] = [];
  searchTerm = '';
  statutFilter: StatutFormation | '' = '';
  loading = true;

  constructor(
    private formationService: FormationService,
    private toast: ToastService,
    private confirmService: ConfirmService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.formationService.getAll().subscribe({
      next: (data) => {
        this.formations = data;
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
  }

  async onDelete(id: number) {
    const ok = await this.confirmService.confirm('Supprimer cette formation ?');
    if (!ok) return;
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
