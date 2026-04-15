import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ExamenService } from '../../services/examen.service';
import { ToastService } from '../../services/toast.service';
import { Examen, PassageExamen } from '../../models/examen.model';

@Component({
  selector: 'app-examen-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './examen-details.component.html',
})
export class ExamenDetailsComponent implements OnInit {
  examen: Examen | null = null;
  resultats: PassageExamen[] = [];
  loading = true;
  loadingResultats = true;

  constructor(
    private examenService: ExamenService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/admin/examens']);
      return;
    }
    const examenId = +id;
    this.examenService.getById(examenId).subscribe({
      next: (e) => {
        this.examen = e;
        this.loading = false;
        this.loadResultats(examenId);
      },
      error: () => {
        this.loading = false;
        this.toast.error('Examen introuvable');
        this.router.navigate(['/admin/examens']);
      },
    });
  }

  loadResultats(examenId: number) {
    this.loadingResultats = true;
    this.examenService.getResultatsByExamen(examenId).subscribe({
      next: (data) => {
        this.resultats = data;
        this.loadingResultats = false;
      },
      error: () => (this.loadingResultats = false),
    });
  }

  resultatClass(r: string): string {
    return r === 'REUSSI' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700';
  }
}
