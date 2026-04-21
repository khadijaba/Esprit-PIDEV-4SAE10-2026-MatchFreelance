import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FreelancerApplicationsComponent } from '../freelancer-applications/freelancer-applications.component';
import { FreelancerContractsComponent } from '../freelancer-contracts/freelancer-contracts.component';
import { AuthService } from '../../services/auth.service';
import { InscriptionService } from '../../services/inscription.service';
import { ExamenService } from '../../services/examen.service';
import { FormationService } from '../../services/formation.service';
import { SkillService } from '../../services/skill.service';
import { UserProfile } from '../../models/auth.model';
import { Skill } from '../../models/skill.model';
import { Inscription } from '../../models/inscription.model';
import {
  PassageExamen,
  Certificat,
  SuccessPrediction,
  RemediationPlan,
  FreelancerRanking,
} from '../../models/examen.model';
import { Formation } from '../../models/formation.model';
import { SKILL_CATEGORY_LABELS } from '../../models/skill.model';

@Component({
  selector: 'app-dashboard-freelancer',
  standalone: true,
  imports: [CommonModule, RouterLink, FreelancerApplicationsComponent, FreelancerContractsComponent],
  templateUrl: './dashboard-freelancer.component.html',
})
export class DashboardFreelancerComponent implements OnInit {
  profile: UserProfile | null = null;
  skills: Skill[] = [];
  inscriptions: Inscription[] = [];
  passages: PassageExamen[] = [];
  certificats: Certificat[] = [];
  formationsOuvertes: Formation[] = [];
  loading = true;
  error: string | null = null;

  rappels: { type: string; message: string; link?: string; linkLabel?: string }[] = [];
  simulation: SuccessPrediction | null = null;
  remediation: RemediationPlan | null = null;

  rankingPreview: FreelancerRanking[] = [];
  rankingPreviewLoading = false;
  rankingPreviewError: string | null = null;

  constructor(
    public auth: AuthService,
    private inscriptionService: InscriptionService,
    private examenService: ExamenService,
    private formationService: FormationService,
    private skillService: SkillService
  ) {}

  get freelancerId(): number | null {
    return this.auth.getStoredUser()?.userId ?? null;
  }

  ngOnInit() {
    if (!this.auth.isLoggedIn() || this.auth.getStoredUser()?.role !== 'FREELANCER') {
      this.error = 'Connectez-vous en tant que Freelancer pour accéder à ce tableau de bord.';
      this.loading = false;
      return;
    }
    const fid = this.freelancerId;
    if (!fid) {
      this.error = 'Profil incomplet. Reconnectez-vous.';
      this.loading = false;
      return;
    }
    this.loadAll(fid);
  }

  loadAll(freelancerId: number) {
    this.loading = true;
    this.rappels = [];

    this.rankingPreviewLoading = true;
    this.rankingPreviewError = null;
    this.examenService.getGlobalRanking().subscribe({
      next: (rows) => {
        this.rankingPreviewLoading = false;
        this.rankingPreview = (rows ?? []).slice(0, 5);
        this.rankingPreviewError = null;
      },
      error: (err) => {
        this.rankingPreviewLoading = false;
        this.rankingPreview = [];
        this.rankingPreviewError =
          err?.error?.message ??
          `Classement indisponible (HTTP ${err?.status ?? '—'}). Vérifiez le microservice Evaluation.`;
      },
    });

    this.auth.getProfile().subscribe({
      next: (p) => (this.profile = p),
      error: () => {},
    });

    this.skillService.getByFreelancerId(freelancerId).subscribe({
      next: (list) => (this.skills = list ?? []),
      error: () => (this.skills = []),
    });

    this.inscriptionService.getByFreelancer(freelancerId).subscribe({
      next: (ins) => {
        this.inscriptions = ins;
        this.loading = false;
        const validees = ins.filter((i) => i.statut === 'VALIDEE');
        if (validees.length > 0) {
          this.rappels.push({
            type: 'examen',
            message: 'Vous avez des formations validées : pensez à passer les examens pour obtenir vos certificats.',
            link: '/mon-activite',
            linkLabel: 'Mon activité',
          });
        }
      },
      error: () => (this.loading = false),
    });

    this.examenService.getResultatsByFreelancer(freelancerId).subscribe({
      next: (p) => {
        this.passages = p;
        const last = [...p].sort(
          (a, b) => new Date(b.datePassage).getTime() - new Date(a.datePassage).getTime()
        )[0];
        if (last?.examenId) {
          this.examenService.getSimulationReussite(last.examenId, freelancerId).subscribe({
            next: (s) => (this.simulation = s),
            error: () => (this.simulation = null),
          });
          this.examenService.getPlanRemediation(last.examenId, freelancerId).subscribe({
            next: (r) => (this.remediation = r),
            error: () => (this.remediation = null),
          });
        }
      },
      error: () => {},
    });

    this.examenService.getCertificatsByFreelancer(freelancerId).subscribe({
      next: (c) => {
        this.certificats = c;
        if (c.length > 0) {
          this.rappels.push({
            type: 'certificat',
            message: `Vous avez ${c.length} certificat(s) obtenu(s).`,
            link: '/mon-activite',
            linkLabel: 'Voir mes certificats',
          });
        }
      },
      error: () => {},
    });

    this.formationService.getOuvertes().subscribe({
      next: (f) => {
        this.formationsOuvertes = f;
        if (f.length > 0) {
          this.rappels.push({
            type: 'formation',
            message: `Vous avez ${f.length} formation(s) ouverte(s) disponible(s).`,
            link: '/formations',
            linkLabel: 'Voir les formations',
          });
        }
      },
      error: () => {},
    });
  }

  categoryLabel(cat: string): string {
    return (SKILL_CATEGORY_LABELS as Record<string, string>)[cat] ?? cat?.replace(/_/g, ' ') ?? '';
  }

  isMyRankingRow(row: FreelancerRanking): boolean {
    const id = this.freelancerId;
    return id != null && row.freelancerId === id;
  }

  statutInscriptionLabel(s: string): string {
    const map: Record<string, string> = {
      EN_ATTENTE: 'En attente',
      VALIDEE: 'Validée',
      REFUSEE: 'Refusée',
      ANNULEE: 'Annulée',
    };
    return map[s] ?? s;
  }
}
