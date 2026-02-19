import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { InscriptionService } from '../../services/inscription.service';
import { ExamenService } from '../../services/examen.service';
import { ToastService } from '../../services/toast.service';
import { Formation, StatutFormation, TypeFormation, TYPE_FORMATION_LABELS } from '../../models/formation.model';
import { Inscription, StatutInscription } from '../../models/inscription.model';

const FREELANCER_ID_KEY = 'freelancerId';

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

  constructor(
    private formationService: FormationService,
    private inscriptionService: InscriptionService,
    private examenService: ExamenService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const stored = localStorage.getItem(FREELANCER_ID_KEY);
    if (stored) this.freelancerIdInput = stored;

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
      },
      error: () => {
        this.loading = false;
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

  get currentFreelancerId(): number | null {
    const n = parseInt(this.freelancerIdInput, 10);
    return Number.isNaN(n) ? null : n;
  }

  get myInscription(): Inscription | null {
    const fid = this.currentFreelancerId;
    if (fid == null) return null;
    return this.inscriptions.find((i) => i.freelancerId === fid) ?? null;
  }

  saveFreelancerId() {
    const n = this.currentFreelancerId;
    if (n == null) {
      this.toast.error("Veuillez entrer un ID freelancer valide.");
      return;
    }
    localStorage.setItem(FREELANCER_ID_KEY, String(n));
    this.toast.success("Profil freelancer enregistré.");
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
        const msg = err?.error?.message ?? err?.message ?? "Erreur lors de l'inscription.";
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
