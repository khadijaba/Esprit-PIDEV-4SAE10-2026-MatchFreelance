import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { InscriptionService } from '../../services/inscription.service';
import { ExamenService } from '../../services/examen.service';
import { SkillService } from '../../services/skill.service';
import { Inscription } from '../../models/inscription.model';
import { PassageExamen, Certificat } from '../../models/examen.model';
import { Skill } from '../../models/skill.model';
import { SKILL_CATEGORY_LABELS } from '../../models/skill.model';

@Component({
  selector: 'app-mon-activite',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mon-activite.component.html',
})
export class MonActiviteComponent implements OnInit {
  inscriptions: Inscription[] = [];
  passages: PassageExamen[] = [];
  certificats: Certificat[] = [];
  skills: Skill[] = [];
  loading = true;
  error: string | null = null;

  constructor(
    public auth: AuthService,
    private inscriptionService: InscriptionService,
    private examenService: ExamenService,
    private skillService: SkillService
  ) {}

  get freelancerId(): number | null {
    return this.auth.getStoredUser()?.userId ?? null;
  }

  ngOnInit() {
    if (!this.auth.isLoggedIn()) {
      this.error = 'Connectez-vous pour voir votre activité.';
      this.loading = false;
      return;
    }
    const fid = this.freelancerId;
    if (!fid) {
      this.error = 'Profil incomplet.';
      this.loading = false;
      return;
    }
    this.inscriptionService.getByFreelancer(fid).subscribe({
      next: (d) => (this.inscriptions = d),
      error: () => {},
    });
    this.examenService.getResultatsByFreelancer(fid).subscribe({
      next: (d) => (this.passages = d),
      error: () => {},
    });
    this.examenService.getCertificatsByFreelancer(fid).subscribe({
      next: (d) => (this.certificats = d),
      error: () => {},
    });
    this.skillService.getByFreelancer(fid).subscribe({
      next: (d) => {
        this.skills = d;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  categoryLabel(cat: string): string {
    return (SKILL_CATEGORY_LABELS as Record<string, string>)[cat] ?? cat?.replace(/_/g, ' ') ?? '';
  }

  statutLabel(s: string): string {
    const map: Record<string, string> = {
      EN_ATTENTE: 'En attente',
      VALIDEE: 'Validée',
      REFUSEE: 'Refusée',
      ANNULEE: 'Annulée',
    };
    return map[s] ?? s;
  }
}
