import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ExamenService } from '../../services/examen.service';
import { ToastService } from '../../services/toast.service';
import { Examen } from '../../models/examen.model';

@Component({
  selector: 'app-examen-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './examen-list.component.html',
})
export class ExamenListComponent implements OnInit {
  examens: Examen[] = [];
  loading = true;

  constructor(
    private examenService: ExamenService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.examenService.getAll().subscribe({
      next: (data) => {
        this.examens = Array.isArray(data) ? data : [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Impossible de charger les examens');
      },
    });
  }

  onDelete(id: number) {
    if (!confirm('Supprimer cet examen ?')) return;
    this.examenService.delete(id).subscribe({
      next: () => {
        this.toast.success('Examen supprimé');
        this.load();
      },
      error: () => this.toast.error('Erreur lors de la suppression'),
    });
  }
}
