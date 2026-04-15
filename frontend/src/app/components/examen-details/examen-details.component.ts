import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ExamenService } from '../../services/examen.service';
import { ToastService } from '../../services/toast.service';
import { Examen, PassageExamen } from '../../models/examen.model';
import {
  MIN_QUESTIONS_PAR_EXAMEN,
  communQuestionsToAddForBothParcours,
  countQuestionsForParcours,
} from '../../utils/examen-parcours.util';

@Component({
  selector: 'app-examen-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './examen-details.component.html',
})
export class ExamenDetailsComponent implements OnInit {
  readonly minQuestionsParcours = MIN_QUESTIONS_PAR_EXAMEN;
  examen: Examen | null = null;
  resultats: PassageExamen[] = [];
  loading = true;
  loadingResultats = true;
  ajoutModeleEnCours = false;

  constructor(
    private examenService: ExamenService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/admin/examens']);
      return;
    }
    const examenId = +id;
    this.examenService.getById(examenId).subscribe({
      next: (e) => {
        this.examen = e;
        this.loading = false;
        this.loadResultats(examenId);
      },
      error: () => {
        this.loading = false;
        this.toast.error('Examen introuvable');
        this.router.navigate(['/admin/examens']);
      },
    });
  }

  loadResultats(examenId: number) {
    this.loadingResultats = true;
    this.examenService.getResultatsByExamen(examenId).subscribe({
      next: (data) => {
        this.resultats = data;
        this.loadingResultats = false;
      },
      error: () => (this.loadingResultats = false),
    });
  }

  resultatClass(r: string): string {
    return r === 'REUSSI' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700';
  }

  countStandard(): number {
    return this.examen?.questions ? countQuestionsForParcours(this.examen.questions, 'STANDARD') : 0;
  }

  countRenforcement(): number {
    return this.examen?.questions ? countQuestionsForParcours(this.examen.questions, 'RENFORCEMENT') : 0;
  }

  parcoursIncomplet(): boolean {
    if (!this.examen?.questions?.length) {
      return true;
    }
    return communQuestionsToAddForBothParcours(this.examen.questions) > 0;
  }

  manquePourPassage(): number {
    if (!this.examen?.questions) {
      return this.minQuestionsParcours;
    }
    return communQuestionsToAddForBothParcours(this.examen.questions);
  }

  ajouterQuestionsCommunModele(): void {
    const n = this.manquePourPassage();
    if (!this.examen || n <= 0) {
      return;
    }
    this.ajoutModeleEnCours = true;
    this.examenService.appendQuestionsModele(this.examen.id, { nombre: n, parcoursInclusion: 'COMMUN' }).subscribe({
      next: (e) => {
        this.examen = e;
        this.ajoutModeleEnCours = false;
        this.toast.success(
          `${n} question(s) Commun ajoutée(s), rédigées à partir des modules de la formation (bonne réponse : A). Vous pouvez les affiner dans l’admin si besoin.`
        );
      },
      error: (err) => {
        this.ajoutModeleEnCours = false;
        const b = err.error;
        const detail =
          (typeof b?.message === 'string' && b.message) ||
          (typeof b?.error === 'string' && b.error && b.error !== 'Not Found' ? b.error : '') ||
          '';
        const msg =
          detail ||
          (typeof err.message === 'string' ? err.message : '') ||
          `Erreur HTTP ${err.status ?? ''}. Redémarrez le microservice Evaluation (port 8083) après compilation.`;
        this.toast.error(msg);
      },
    });
  }
}
