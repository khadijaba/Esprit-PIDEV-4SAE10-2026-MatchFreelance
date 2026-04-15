import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Certificat } from '../../models/examen.model';

/**
 * Affichage du certificat (aligné sur le PDF : seuil, mention, ADMIS).
 */
@Component({
  selector: 'app-certificat-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './certificat-display.component.html',
})
export class CertificatDisplayComponent {
  @Input({ required: true }) certificat!: Certificat;
  @Input() showVerify = true;
  /** Fichier dans `frontend/public/` (ex. `/logo-matchfreelance.png`). */
  @Input() logoSrc = '/logo-matchfreelance.png';

  logoError = false;

  get seuil(): number {
    return this.certificat.seuilReussi ?? 60;
  }

  get mention(): string {
    const s = this.certificat.score ?? 0;
    if (s >= 85) return 'Très bien';
    if (s >= 70) return 'Bien';
    if (s >= 60) return 'Acceptable';
    return 'Refus';
  }

  get decision(): 'ADMIS' | 'REFUSÉ' {
    const s = this.certificat.score ?? 0;
    return s >= this.seuil ? 'ADMIS' : 'REFUSÉ';
  }

  get verifyPageUrl(): string {
    const base = typeof window !== 'undefined' ? window.location.origin : '';
    const num = this.certificat.numeroCertificat ?? '';
    return `${base}/api/certificats/verify/${encodeURIComponent(num)}/page`;
  }

  get qrImageUrl(): string {
    const data = encodeURIComponent(this.verifyPageUrl);
    return `https://api.qrserver.com/v1/create-qr-code/?size=140x140&data=${data}`;
  }

  onLogoError(): void {
    this.logoError = true;
  }
}
