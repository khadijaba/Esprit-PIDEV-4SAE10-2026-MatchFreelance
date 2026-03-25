import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ExamenService } from '../../services/examen.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { Certificat, Examen, PassageExamen } from '../../models/examen.model';
import { CertificatDisplayComponent } from '../certificat-display/certificat-display.component';

const FREELANCER_ID_KEY = 'freelancerId';

@Component({
  selector: 'app-passer-examen',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CertificatDisplayComponent],
  templateUrl: './passer-examen.component.html',
})
export class PasserExamenComponent implements OnInit {
  formationId = 0;
  examen: Examen | null = null;
  reponses: string[] = [];
  loading = true;
  submitting = false;
  resultat: PassageExamen | null = null;
  certificat: Certificat | null = null;
  certificatLoading = false;
  freelancerIdInput = '';

  constructor(
    private examenService: ExamenService,
    private toast: ToastService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const fid = this.route.snapshot.paramMap.get('formationId');
    const eid = this.route.snapshot.paramMap.get('examenId');
    if (fid) this.formationId = +fid;
    if (!eid) {
      this.router.navigate(['/formations']);
      return;
    }
    const user = this.auth.getStoredUser();
    if (user?.role === 'FREELANCER' && user.userId) {
      this.freelancerIdInput = String(user.userId);
      localStorage.setItem(FREELANCER_ID_KEY, String(user.userId));
    } else {
      const stored = localStorage.getItem(FREELANCER_ID_KEY);
      if (stored) this.freelancerIdInput = stored;
    }

    const examenId = +eid;
    this.examenService.getPourPassage(examenId).subscribe({
      next: (e) => {
        this.examen = e;
        this.reponses = (e.questions ?? []).map(() => '');
        this.loading = false;
        const fid = this.currentFreelancerId;
        if (fid != null) {
          this.examenService.getPassage(examenId, fid).subscribe({
            next: (passage) => {
              this.resultat = passage;
              if (passage.resultat === 'REUSSI' && passage.certificat) {
                this.certificat = passage.certificat;
              } else if (passage.resultat === 'REUSSI') {
                this.loadCertificat(passage.id);
              }
            },
            error: () => {},
          });
        }
      },
      error: () => {
        this.loading = false;
        this.toast.error('Examen introuvable');
        this.router.navigate(['/formations', this.formationId || '']);
      },
    });
  }

  get currentFreelancerId(): number | null {
    const n = parseInt(this.freelancerIdInput, 10);
    return Number.isNaN(n) ? null : n;
  }

  get isFreelancerConnected(): boolean {
    const u = this.auth.getStoredUser();
    return u?.role === 'FREELANCER' && !!u.userId;
  }

  get connectedFreelancerId(): number | null {
    const u = this.auth.getStoredUser();
    return u?.role === 'FREELANCER' && u.userId ? u.userId : null;
  }

  canSubmit(): boolean {
    if (!this.examen?.questions?.length || !this.currentFreelancerId) return false;
    return this.reponses.every((r) => r && r.length > 0);
  }

  submit() {
    const fid = this.currentFreelancerId;
    if (!fid || !this.examen) return;
    const rep = this.reponses.map((r) => (r ?? '').toUpperCase().trim().substring(0, 1) || 'A');
    this.submitting = true;
    this.examenService.passerExamen(this.examen.id, { freelancerId: fid, reponses: rep }).subscribe({
      next: (p) => {
        this.resultat = p;
        this.submitting = false;
        if (p.freelancerId) localStorage.setItem(FREELANCER_ID_KEY, String(p.freelancerId));
        this.toast.success(p.resultat === 'REUSSI' ? 'Examen réussi ! Certificat délivré.' : 'Examen terminé.');
        if (p.resultat === 'REUSSI') {
          if (p.certificat) {
            this.certificat = p.certificat;
          } else {
            this.loadCertificat(p.id);
          }
        }
      },
      error: (err) => {
        this.submitting = false;
        this.toast.error(err.error?.message ?? 'Erreur lors de la soumission');
      },
    });
  }

  trackByIndex(i: number) {
    return i;
  }

  private loadCertificat(passageId: number) {
    this.certificatLoading = true;
    this.examenService.getCertificatByPassage(passageId).subscribe({
      next: (c) => {
        this.certificat = c;
        this.certificatLoading = false;
      },
      error: () => {
        this.certificatLoading = false;
      },
    });
  }

  openCertificatPdf() {
    if (!this.certificat?.id) return;
    this.examenService.getCertificatPdf(this.certificat.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
      },
      error: () => {},
    });
  }

  /** Ouvre la page certificat en lui passant les données pour éviter « Certificat introuvable ». */
  goToCertificatView() {
    if (!this.certificat?.id) return;
    this.router.navigate(['/certificat', this.certificat.id], { state: { certificat: this.certificat } });
  }
}
