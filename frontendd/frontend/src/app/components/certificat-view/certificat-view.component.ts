import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ExamenService } from '../../services/examen.service';
import { Certificat } from '../../models/examen.model';

@Component({
  selector: 'app-certificat-view',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './certificat-view.component.html',
})
export class CertificatViewComponent implements OnInit {
  certificat: Certificat | null = null;
  loading = true;
  error = false;

  constructor(
    private examenService: ExamenService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  certificatIdFromRoute: number | null = null;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/formations']);
      return;
    }
    const numId = +id;
    if (!Number.isNaN(numId)) this.certificatIdFromRoute = numId;

    // Si on arrive depuis la page "résultat examen", le certificat est passé en state → pas d'appel API
    const navState = this.router.getCurrentNavigation()?.extras?.state ?? history.state;
    const state = navState as { certificat?: Certificat } | undefined;
    if (state?.certificat && state.certificat.id === numId) {
      this.certificat = state.certificat;
      this.loading = false;
      return;
    }
    // Sinon (lien direct, favori, etc.) on charge depuis l'API
    this.examenService.getCertificatById(numId).subscribe({
      next: (c) => {
        this.certificat = c;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
      },
    });
  }

  print() {
    window.print();
  }

  /** Ouvre le certificat en PDF dans un nouvel onglet (charge via HttpClient pour passer par le proxy). */
  openPdf() {
    if (!this.certificat?.id) return;
    this.examenService.getCertificatPdf(this.certificat.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
      },
      error: () => {},
    });
  }

  /** Ouvre le PDF par id (utile quand la page certificat affiche une erreur). */
  openPdfById(id: number) {
    this.examenService.getCertificatPdf(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
      },
      error: () => {},
    });
  }
}
