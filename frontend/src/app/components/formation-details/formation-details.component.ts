import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { ExamenService } from '../../services/examen.service';
import { InscriptionService } from '../../services/inscription.service';
import { ToastService } from '../../services/toast.service';
import { Formation, StatutFormation, TypeFormation, TYPE_FORMATION_LABELS } from '../../models/formation.model';
import { Examen } from '../../models/examen.model';
import { Inscription, StatutInscription } from '../../models/inscription.model';

@Component({
  selector: 'app-formation-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './formation-details.component.html',
})
export class FormationDetailsComponent implements OnInit {
  formation: Formation | null = null;
  examens: Examen[] = [];
  inscriptions: Inscription[] = [];
  loading = true;
  examensLoading = false;
  inscriptionsLoading = false;

  constructor(
    private formationService: FormationService,
    private examenService: ExamenService,
    private inscriptionService: InscriptionService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/admin/formations']);
      return;
    }
    this.formationService.getById(+id).subscribe({
      next: (f) => {
        this.formation = f;
        this.loading = false;
        if (f?.id) {
          this.loadExamens(f.id);
          this.loadInscriptions(f.id);
        }
      },
      error: () => {
        this.loading = false;
        this.toast.error('Formation introuvable');
        this.router.navigate(['/admin/formations']);
      },
    });
  }

  private loadExamens(formationId: number) {
    this.examensLoading = true;
    this.examenService.getByFormation(formationId).subscribe({
      next: (list) => {
        this.examens = list;
        this.examensLoading = false;
      },
      error: () => (this.examensLoading = false),
    });
  }

  private loadInscriptions(formationId: number) {
    this.inscriptionsLoading = true;
    this.inscriptionService.getByFormation(formationId).subscribe({
      next: (list) => {
        this.inscriptions = list;
        this.inscriptionsLoading = false;
      },
      error: () => (this.inscriptionsLoading = false),
    });
  }

  validerInscription(ins: Inscription) {
    this.inscriptionService.valider(ins.id).subscribe({
      next: () => {
        this.toast.success('Inscription validée.');
        if (this.formation?.id) this.loadInscriptions(this.formation.id);
      },
      error: () => this.toast.error('Impossible de valider.'),
    });
  }

  annulerInscription(ins: Inscription) {
    if (!confirm('Annuler cette inscription ?')) return;
    this.inscriptionService.annuler(ins.id).subscribe({
      next: () => {
        this.toast.success('Inscription annulée.');
        if (this.formation?.id) this.loadInscriptions(this.formation.id);
      },
      error: () => this.toast.error('Impossible d\'annuler.'),
    });
  }

  statutInscriptionClass(s: StatutInscription): string {
    const map: Record<StatutInscription, string> = {
      EN_ATTENTE: 'bg-amber-100 text-amber-700',
      VALIDEE: 'bg-emerald-100 text-emerald-700',
      REFUSEE: 'bg-red-100 text-red-700',
      ANNULEE: 'bg-slate-100 text-slate-600',
    };
    return map[s] ?? 'bg-gray-100 text-gray-700';
  }

  typeFormationLabel(t?: TypeFormation): string {
    return t ? (TYPE_FORMATION_LABELS[t] ?? t) : '—';
  }

  statutClass(s: StatutFormation): string {
    const map: Record<StatutFormation, string> = {
      OUVERTE: 'bg-emerald-100 text-emerald-700',
      EN_COURS: 'bg-amber-100 text-amber-700',
      TERMINEE: 'bg-blue-100 text-blue-700',
      ANNULEE: 'bg-red-100 text-red-700',
    };
    return map[s] ?? 'bg-gray-100 text-gray-700';
  }
}
