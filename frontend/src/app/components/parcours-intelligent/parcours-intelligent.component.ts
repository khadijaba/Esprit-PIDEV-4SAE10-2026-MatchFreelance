import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ParcoursService } from '../../services/parcours.service';
import { AuthService } from '../../services/auth.service';
import { FormationService } from '../../services/formation.service';
import { InscriptionService } from '../../services/inscription.service';
import { ToastService } from '../../services/toast.service';
import { FormationProposeeDto, ParcoursIntelligentResponse } from '../../models/parcours.model';
import { Formation, TYPE_FORMATION_LABELS, TypeFormation } from '../../models/formation.model';

@Component({
  selector: 'app-parcours-intelligent',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './parcours-intelligent.component.html',
})
export class ParcoursIntelligentComponent implements OnInit {
  freelancerIdInput = '';
  data: ParcoursIntelligentResponse | null = null;
  /** Formations ouvertes (mode sans Skill) affichées avec bouton d’inscription. */
  formationsDecouverte: FormationProposeeDto[] = [];
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

  ngOnInit(): void {
    if (this.effectiveFreelancerId != null) {
      this.loadParcours();
    }
  }

  /** ID utilisé pour le parcours : utilisateur connecté (front) ou champ si présent (admin/démo). */
  get effectiveFreelancerId(): number | null {
    const user = this.auth.getStoredUser();
    if (user?.role === 'FREELANCER' && user.userId) return user.userId;
    const parsed = parseInt(this.freelancerIdInput, 10);
    return Number.isNaN(parsed) || parsed < 1 ? null : parsed;
  }

  /** Formations de la section principale : gaps métier ou catalogue ouvert (mode dégradé). */
  get formationsCibleesAffichees(): FormationProposeeDto[] {
    const prop = this.data?.formationsProposees;
    if (prop && prop.length > 0) return prop;
    return this.formationsDecouverte;
  }

  get modeSansAnalyseCompetences(): boolean {
    return this.data?.analyseCompetencesDisponible === false;
  }

  loadParcours() {
    const id = this.effectiveFreelancerId;
    if (id == null) {
      this.error = 'Connectez-vous en tant que freelancer pour analyser votre parcours.';
      return;
    }
    this.error = null;
    this.data = null;
    this.formationsDecouverte = [];
    this.formationsAllerPlusLoin = [];
    this.inscritFormationIds = new Set();
    this.loading = true;
    this.parcoursService.getParcoursIntelligent(id).subscribe({
      next: (res) => {
        this.data = res;
        this.loading = false;
        if (res.analyseCompetencesDisponible === false) {
          this.formationService.getOuvertes().subscribe({
            next: (list) => {
              const ouvertes = list
                .filter((f) => f.statut === 'OUVERTE')
                .sort((a, b) => a.titre.localeCompare(b.titre, 'fr', { sensitivity: 'base' }));
              this.formationsDecouverte = ouvertes.slice(0, 24).map((f) => ({
                id: f.id,
                titre: f.titre,
                typeFormation: f.typeFormation ?? '',
                description: f.description ?? undefined,
                dureeHeures: f.dureeHeures,
                statut: f.statut,
              }));
            },
            error: () => {},
          });
        } else {
          this.loadFormationsAllerPlusLoin(res.categoriesActuelles);
          if (!res.categoriesActuelles.length) {
            this.formationService.getOuvertes().subscribe({
              next: (list) => {
                this.formationsAllerPlusLoin = list.slice(0, 12);
              },
              error: () => {},
            });
          }
        }
        this.loadInscriptions(id);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message ?? err?.message ?? 'Impossible de charger le parcours.';
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

  /** Libellé français pour les types connus (enum), sinon chaîne lisible. */
  libelleTypeFormation(raw: string | undefined | null): string {
    if (raw == null || raw === '') return '—';
    if (Object.prototype.hasOwnProperty.call(TYPE_FORMATION_LABELS, raw)) {
      return TYPE_FORMATION_LABELS[raw as TypeFormation];
    }
    return this.formatCategory(raw);
  }
}
