import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ParcoursService } from '../../services/parcours.service';
import { AuthService } from '../../services/auth.service';
import { FormationService } from '../../services/formation.service';
import { InscriptionService } from '../../services/inscription.service';
import { ToastService } from '../../services/toast.service';
import { ParcoursIntelligentResponse } from '../../models/parcours.model';
import { Formation } from '../../models/formation.model';

@Component({
  selector: 'app-parcours-intelligent',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './parcours-intelligent.component.html',
})
export class ParcoursIntelligentComponent {
  freelancerIdInput = '';
  data: ParcoursIntelligentResponse | null = null;
  formationsAllerPlusLoin: Formation[] = [];
  loading = false;
  error: string | null = null;
  inscribingId: number | null = null;

  constructor(
    private parcoursService: ParcoursService,
    public auth: AuthService,
    private formationService: FormationService,
    private inscriptionService: InscriptionService,
    private toast: ToastService
  ) {
    const user = this.auth.getStoredUser();
    if (user?.role === 'FREELANCER' && user.userId) {
      this.freelancerIdInput = String(user.userId);
    }
  }

  loadParcours() {
    const id = parseInt(this.freelancerIdInput, 10);
    if (Number.isNaN(id) || id < 1) {
      this.error = 'Entrez un ID freelancer valide.';
      return;
    }
    this.error = null;
    this.data = null;
    this.formationsAllerPlusLoin = [];
    this.loading = true;
    this.parcoursService.getParcoursIntelligent(id).subscribe({
      next: (res) => {
        this.data = res;
        this.loading = false;
        this.loadFormationsAllerPlusLoin(res.categoriesActuelles);
      },
      error: (err) => {
        this.loading = false;
        const status = err?.status ?? err?.statusCode;
        if (status === 404 || status === 503) {
          const base = 'Le microservice Parcours Intelligent (Skill) ne répond pas';
          const hint = status === 503
            ? ' (503). Vérifiez qu\'Eureka et la Gateway voient SKILL, puis réessayez.'
            : ' (404). Vérifiez que Skill est démarré et que la Gateway route /api/skills vers SKILL.';
          this.error = base + hint + ' Test du routage : ouvrez http://localhost:8050/api/skills/ping dans le navigateur.';
        } else {
          this.error = err?.error?.message ?? err?.message ?? 'Impossible de charger le parcours.';
        }
      },
    });
  }

  private loadFormationsAllerPlusLoin(categoriesActuelles: string[]) {
    if (categoriesActuelles.length === 0) return;
    this.formationService.getOuvertes().subscribe({
      next: (list) => {
        this.formationsAllerPlusLoin = list.filter(
          (f) => f.typeFormation && categoriesActuelles.includes(f.typeFormation)
        );
      },
      error: () => {},
    });
  }

  inscrire(formationId: number) {
    const fid = this.data?.freelancerId ?? parseInt(this.freelancerIdInput, 10);
    if (Number.isNaN(fid) || fid < 1) {
      this.toast.error('ID freelancer invalide.');
      return;
    }
    this.inscribingId = formationId;
    this.inscriptionService.inscrire(formationId, fid).subscribe({
      next: () => {
        this.inscribingId = null;
        this.toast.success('Inscription envoyée. En attente de validation.');
      },
      error: (err) => {
        this.inscribingId = null;
        this.toast.error(err?.error?.message ?? err?.message ?? 'Erreur lors de l\'inscription.');
      },
    });
  }

  formatCategory(cat: string): string {
    return cat.replace(/_/g, ' ');
  }
}
