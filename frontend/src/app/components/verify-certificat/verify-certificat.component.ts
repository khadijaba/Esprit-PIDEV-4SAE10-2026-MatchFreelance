import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CertificatVerifyResponse } from '../../models/examen.model';

/**
 * Page publique ouverte par le QR code du certificat (scan mobile).
 */
@Component({
  selector: 'app-verify-certificat',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './verify-certificat.component.html',
})
export class VerifyCertificatComponent implements OnInit {
  loading = true;
  data: CertificatVerifyResponse | null = null;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((pm) => {
      const raw = pm.get('numero');
      const numero = raw ? decodeURIComponent(raw) : '';
      if (!numero) {
        this.loading = false;
        this.data = { valid: false, message: 'Numéro de certificat manquant.' };
        return;
      }
      this.http.get<CertificatVerifyResponse>('/api/certificats/verify/' + encodeURIComponent(numero)).subscribe({
        next: (d) => {
          this.data = d;
          this.loading = false;
        },
        error: (err) => {
          this.loading = false;
          this.data = {
            valid: false,
            numeroCertificat: numero,
            message:
              err?.error?.message ??
              'Impossible de joindre le serveur. Vérifiez que la Gateway (8050) tourne.',
          };
        },
      });
    });
  }

  formationLink(id: unknown): string {
    if (id == null) return '/formations';
    return '/formations/' + id;
  }
}
