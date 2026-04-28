import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { InscriptionService } from '../../services/inscription.service';
import { ExamenService } from '../../services/examen.service';
import { ModuleService } from '../../services/module.service';
import { ToastService } from '../../services/toast.service';
import { AuthService, FREELANCER_ID_STORAGE_KEY } from '../../services/auth.service';
import {
  Formation,
  NiveauFormation,
  NIVEAU_FORMATION_LABELS,
  StatutFormation,
  TypeFormation,
  TYPE_FORMATION_LABELS,
} from '../../models/formation.model';
import { Inscription, StatutInscription } from '../../models/inscription.model';

@Component({
  selector: 'app-front-formation-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './front-formation-detail.component.html',
})
export class FrontFormationDetailComponent implements OnInit {
  formation: Formation | null = null;
  loading = true;
  inscriptions: Inscription[] = [];
  inscriptionLoading = false;
  inscriptionSaving = false;
  freelancerIdInput = '';
  examens: { id: number; titre: string }[] = [];
  examensLoading = false;
  /** IDs des examens déjà passés par le freelancer connecté (bouton « Passer l'examen » désactivé). */
  examensDejaPassesIds = new Set<number>();
  resultatsLoading = false;
  modules: { id: number; titre: string; description: string | null; dureeMinutes: number; ordre: number }[] = [];
  modulesLoading = false;
  /** Titre de l'examen requis (certificat) pour afficher la condition d'accès. */
  examenRequisTitre: string | null = null;

  /** Expose AuthService au template pour afficher "Connecté en tant que Freelancer". */
  get authServiceForTemplate(): AuthService {
    return this.auth;
  }

  get isFreelancerSession(): boolean {
    const u = this.auth.getStoredUser();
    return u?.role === 'FREELANCER' && u.userId != null;
  }

  constructor(
    private formationService: FormationService,
    private inscriptionService: InscriptionService,
    private examenService: ExamenService,
    private moduleService: ModuleService,
    private toast: ToastService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const user = this.auth.getStoredUser();
    if (user?.role === 'FREELANCER' && user.userId != null) {
      this.freelancerIdInput = String(user.userId);
      localStorage.setItem(FREELANCER_ID_STORAGE_KEY, String(user.userId));
    } else {
      const stored = localStorage.getItem(FREELANCER_ID_STORAGE_KEY);
      if (stored) this.freelancerIdInput = stored;
    }

    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/formations']);
      return;
    }
    const formationId = +id;
    this.formationService.getById(formationId).subscribe({
      next: (f) => {
        this.formation = f;
        this.loading = false;
        this.loadInscriptions(formationId);
        this.loadExamens(formationId);
        this.loadModules(formationId);
        this.loadResultatsFreelancer();
        if (f.examenRequisId != null) {
          this.examenService.getById(f.examenRequisId).subscribe({
            next: (ex) => (this.examenRequisTitre = ex.titre),
            error: () => (this.examenRequisTitre = 'examen #' + f.examenRequisId),
          });
        } else {
          this.examenRequisTitre = null;
        }
      },
      error: (err) => {
        this.loading = false;
        const status = err?.status;
        if (status === 404) {
          this.toast.error('Formation introuvable.');
        } else if (status === 0) {
          this.toast.error(
            'Service Formation indisponible. Vérifiez que le microservice Formation et la Gateway sont démarrés.'
          );
        } else {
          const msg = err?.error?.message ?? err?.error?.error ?? 'Impossible de charger la formation.';
          this.toast.error(msg);
        }
        this.router.navigate(['/formations']);
      },
    });
  }

  private loadInscriptions(formationId: number) {
    this.inscriptionLoading = true;
    this.inscriptionService.getByFormation(formationId).subscribe({
      next: (list) => {
        this.inscriptions = list;
        this.inscriptionLoading = false;
      },
      error: () => (this.inscriptionLoading = false),
    });
  }

  private loadExamens(formationId: number) {
    this.examensLoading = true;
    this.examenService.getByFormation(formationId).subscribe({
      next: (list) => {
        this.examens = list.map((e) => ({ id: e.id, titre: e.titre }));
        this.examensLoading = false;
      },
      error: () => (this.examensLoading = false),
    });
  }

  private loadModules(formationId: number) {
    this.modulesLoading = true;
    this.moduleService.getByFormation(formationId).subscribe({
      next: (list) => {
        this.modules = list.map((m) => ({
          id: m.id,
          titre: m.titre,
          description: m.description,
          dureeMinutes: m.dureeMinutes,
          ordre: m.ordre,
        }));
        this.modulesLoading = false;
      },
      error: () => (this.modulesLoading = false),
    });
  }

  private loadResultatsFreelancer() {
    const fid = this.currentFreelancerId;
    if (fid == null) {
      this.examensDejaPassesIds = new Set();
      return;
    }
    this.resultatsLoading = true;
    this.examenService.getResultatsByFreelancer(fid).subscribe({
      next: (list) => {
        this.examensDejaPassesIds = new Set(list.map((r) => r.examenId));
        this.resultatsLoading = false;
      },
      error: () => {
        this.examensDejaPassesIds = new Set();
        this.resultatsLoading = false;
      },
    });
  }

  isExamenDejaPasse(examenId: number): boolean {
    return this.examensDejaPassesIds.has(examenId);
  }

  get currentFreelancerId(): number | null {
    const u = this.auth.getStoredUser();
    if (u?.role === 'FREELANCER' && u.userId != null) {
      return u.userId;
    }
    const n = parseInt(this.freelancerIdInput, 10);
    return Number.isNaN(n) ? null : n;
  }

  get myInscription(): Inscription | null {
    const fid = this.currentFreelancerId;
    if (fid == null) return null;
    return this.inscriptions.find((i) => i.freelancerId === fid) ?? null;
  }

  saveFreelancerId() {
    if (this.isFreelancerSession) {
      this.toast.info("Vous êtes connecté : l'ID utilisé est celui de votre compte.");
      return;
    }
    const n = parseInt(this.freelancerIdInput, 10);
    if (Number.isNaN(n) || n < 1) {
      this.toast.error("Veuillez entrer un ID freelancer valide.");
      return;
    }
    localStorage.setItem(FREELANCER_ID_STORAGE_KEY, String(n));
    this.toast.success("Profil freelancer enregistré.");
    this.loadResultatsFreelancer();
  }

  inscrire() {
    const fid = this.currentFreelancerId;
    if (fid == null || !this.formation) {
      this.toast.error("Entrez votre ID freelancer puis cliquez sur S'inscrire.");
      return;
    }
    this.inscriptionSaving = true;
    this.inscriptionService.inscrire(this.formation.id, fid).subscribe({
      next: () => {
        this.inscriptionSaving = false;
        this.toast.success("Inscription envoyée. Statut : En attente.");
        this.loadInscriptions(this.formation!.id);
      },
      error: (err) => {
        this.inscriptionSaving = false;
        const msg = err?.error?.error ?? err?.error?.message ?? err?.message ?? "Erreur lors de l'inscription.";
        this.toast.error(msg);
      },
    });
  }

  annulerInscription() {
    const ins = this.myInscription;
    if (!ins || !this.formation) return;
    if (!confirm("Annuler votre inscription à cette formation ?")) return;
    this.inscriptionService.annuler(ins.id).subscribe({
      next: () => {
        this.toast.success("Inscription annulée.");
        this.loadInscriptions(this.formation!.id);
      },
      error: () => this.toast.error("Impossible d'annuler l'inscription."),
    });
  }

  typeFormationLabel(t?: TypeFormation): string {
    return t ? (TYPE_FORMATION_LABELS[t] ?? t) : '';
  }

  niveauLabel(n?: NiveauFormation | null): string {
    return n ? (NIVEAU_FORMATION_LABELS[n] ?? n) : '';
  }

  statutClass(s: StatutFormation): string {
    const map: Record<StatutFormation, string> = {
      OUVERTE: 'bg-emerald-100 text-emerald-700',
      EN_COURS: 'bg-amber-100 text-amber-700',
      TERMINEE: 'bg-blue-100 text-blue-700',
      ANNULEE: 'bg-red-100 text-red-700',
    };
    return map[s] ?? 'bg-slate-100 text-slate-600';
  }

  statutInscriptionClass(s: StatutInscription): string {
    const map: Record<StatutInscription, string> = {
      EN_ATTENTE: 'bg-amber-100 text-amber-700',
      VALIDEE: 'bg-emerald-100 text-emerald-700',
      REFUSEE: 'bg-red-100 text-red-700',
      ANNULEE: 'bg-slate-100 text-slate-600',
    };
    return map[s] ?? 'bg-slate-100 text-slate-600';
  }
}
