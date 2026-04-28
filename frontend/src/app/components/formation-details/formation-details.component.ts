import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { ExamenService } from '../../services/examen.service';
import { InscriptionService } from '../../services/inscription.service';
import { ModuleService } from '../../services/module.service';
import { ToastService } from '../../services/toast.service';
import {
  Formation,
  NiveauFormation,
  NIVEAU_FORMATION_LABELS,
  StatutFormation,
  TypeFormation,
  TYPE_FORMATION_LABELS,
} from '../../models/formation.model';
import { Examen, ExamenDraft, QuestionDto } from '../../models/examen.model';
import { Inscription, StatutInscription } from '../../models/inscription.model';
import { Module } from '../../models/module.model';

@Component({
  selector: 'app-formation-details',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './formation-details.component.html',
})
export class FormationDetailsComponent implements OnInit {
  formation: Formation | null = null;
  examens: Examen[] = [];
  inscriptions: Inscription[] = [];
  modules: Module[] = [];
  loading = true;
  examensLoading = false;
  inscriptionsLoading = false;
  modulesLoading = false;
  moduleSaving = false;
  generatingExamen = false;
  confirmingExamen = false;
  /** Brouillon affiché dans la modale (prévisualisation avant enregistrement). */
  draftGeneratedExamen: ExamenDraft | null = null;
  /** Active l’appel LLM (Ollama / OpenAI) côté serveur si configuré. */
  useLlmForAutoGenerate = true;
  newModule = { titre: '', description: '', dureeMinutes: 15, ordre: 0 };

  constructor(
    private formationService: FormationService,
    private examenService: ExamenService,
    private inscriptionService: InscriptionService,
    private moduleService: ModuleService,
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
          this.loadModules(f.id);
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

  private loadModules(formationId: number) {
    this.modulesLoading = true;
    this.moduleService.getByFormation(formationId).subscribe({
      next: (list) => {
        this.modules = list;
        this.modulesLoading = false;
      },
      error: () => (this.modulesLoading = false),
    });
  }

  /** Génère un brouillon d’examen (prévisualisation) : enregistrement après validation. */
  generateExamenFromModules() {
    if (!this.formation?.id) return;
    if (this.modules.length === 0) {
      this.toast.error('Ajoutez au moins un module à cette formation avant de générer un examen.');
      return;
    }
    this.runPreviewGeneration();
  }

  private runPreviewGeneration() {
    if (!this.formation?.id) return;
    this.generatingExamen = true;
    this.examenService
      .previewGenerateFromFormation(this.formation.id, {
        seuilReussi: 60,
        useLlm: this.useLlmForAutoGenerate,
      })
      .subscribe({
      next: (draft) => {
        this.generatingExamen = false;
        this.draftGeneratedExamen = draft;
        this.toast.success('Prévisualisation prête : vérifiez les questions puis validez ou régénérez.');
      },
      error: (err) => {
        this.generatingExamen = false;
        const msg =
          err.error?.message ??
          err.message ??
          'Impossible de générer l’examen (vérifiez Gateway, Evaluation et Formation).';
        this.toast.error(msg);
      },
    });
  }

  closeDraftPreview() {
    this.draftGeneratedExamen = null;
  }

  regenerateDraftExamen() {
    this.runPreviewGeneration();
  }

  confirmDraftExamen() {
    const draft = this.draftGeneratedExamen;
    if (!draft || !this.formation?.id) return;
    this.confirmingExamen = true;
    this.examenService.confirmAutoGenerated(draft).subscribe({
      next: (saved) => {
        this.confirmingExamen = false;
        this.draftGeneratedExamen = null;
        this.toast.success('Examen enregistré.');
        this.loadExamens(this.formation!.id!);
        if (saved.id != null) {
          this.router.navigate(['/admin/examens', saved.id]);
        }
      },
      error: (err) => {
        this.confirmingExamen = false;
        const msg = err.error?.message ?? err.message ?? 'Enregistrement impossible.';
        this.toast.error(msg);
      },
    });
  }

  optionLabel(q: QuestionDto, letter: 'A' | 'B' | 'C' | 'D'): string {
    const v =
      letter === 'A' ? q.optionA : letter === 'B' ? q.optionB : letter === 'C' ? q.optionC : q.optionD;
    return v ?? '—';
  }

  addModule() {
    if (!this.formation?.id || !this.newModule.titre.trim()) {
      this.toast.error('Titre du module requis.');
      return;
    }
    this.moduleSaving = true;
    const duree = Number(this.newModule.dureeMinutes) || 15;
    const ordre = Number(this.newModule.ordre) >= 0 ? Number(this.newModule.ordre) : this.modules.length;
    this.moduleService.create({
      titre: this.newModule.titre.trim(),
      description: (this.newModule.description && this.newModule.description.trim()) || '',
      dureeMinutes: duree < 1 ? 15 : duree,
      ordre,
      formationId: this.formation.id,
    }).subscribe({
      next: () => {
        this.moduleSaving = false;
        this.newModule = { titre: '', description: '', dureeMinutes: 15, ordre: this.modules.length };
        this.toast.success('Module ajouté.');
        this.loadModules(this.formation!.id);
      },
      error: (err) => {
        this.moduleSaving = false;
        let msg: string;
        if (err?.status === 404) {
          msg = 'Ressource introuvable.';
        } else if (err?.status === 0 || !(err?.error?.error || err?.error?.message || err?.message)) {
          msg =
            'Connexion impossible. Utilisez http://localhost:4200 (ng serve), vérifiez que Formation tourne (port 8096) et que la Gateway est démarrée.';
        } else {
          msg = err?.error?.error || err?.error?.message || err?.message;
        }
        this.toast.error(msg);
      },
    });
  }

  deleteModule(m: Module) {
    if (!confirm('Supprimer le module « ' + m.titre + ' » ?')) return;
    this.moduleService.delete(m.id).subscribe({
      next: () => {
        this.toast.success('Module supprimé.');
        if (this.formation?.id) this.loadModules(this.formation.id);
      },
      error: () => this.toast.error('Impossible de supprimer.'),
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

  niveauLabel(n?: NiveauFormation | null): string {
    return n ? (NIVEAU_FORMATION_LABELS[n] ?? n) : '—';
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
