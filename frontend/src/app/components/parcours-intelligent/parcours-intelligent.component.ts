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
  /** IDs des formations où le freelancer est déjà inscrit (bouton grisé "Inscrit"). */
  inscritFormationIds = new Set<number>();

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

  /** ID utilisé pour le parcours : utilisateur connecté (front) ou champ si présent (admin/démo). */
  get effectiveFreelancerId(): number | null {
    const user = this.auth.getStoredUser();
    if (user?.role === 'FREELANCER' && user.userId) return user.userId;
    const parsed = parseInt(this.freelancerIdInput, 10);
    return Number.isNaN(parsed) || parsed < 1 ? null : parsed;
  }

  loadParcours() {
    const id = this.effectiveFreelancerId;
    if (id == null) {
      this.error = 'Connectez-vous en tant que freelancer pour analyser votre parcours.';
      return;
    }
    this.error = null;
    this.data = null;
    this.formationsAllerPlusLoin = [];
    this.inscritFormationIds = new Set();
    this.loading = true;
    this.parcoursService.getParcoursIntelligent(id).subscribe({
      next: (res) => {
        this.data = res;
        this.loading = false;
        this.loadFormationsAllerPlusLoin(res.categoriesActuelles);
        this.loadInscriptions(id);
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

  private loadInscriptions(freelancerId: number) {
    this.inscriptionService.getByFreelancer(freelancerId).subscribe({
      next: (list) => {
        list.forEach((i) => this.inscritFormationIds.add(i.formationId));
      },
      error: () => {},
    });
  }

  isInscrit(formationId: number): boolean {
    return this.inscritFormationIds.has(formationId);
  }

  inscrire(formationId: number) {
    const fid = this.data?.freelancerId ?? this.effectiveFreelancerId;
    if (fid == null || fid < 1) {
      this.toast.error('Connectez-vous pour vous inscrire à une formation.');
      return;
    }
    this.inscribingId = formationId;
    this.inscriptionService.inscrire(formationId, fid).subscribe({
      next: () => {
        this.inscribingId = null;
        this.inscritFormationIds.add(formationId);
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
