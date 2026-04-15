import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ExamenService } from '../../services/examen.service';
import { FormationService } from '../../services/formation.service';
import { ToastService } from '../../services/toast.service';
import {
  ExamenRequest,
  QuestionDto,
  QuestionValidationResult,
} from '../../models/examen.model';
import { Formation } from '../../models/formation.model';
import {
  MIN_QUESTIONS_PAR_EXAMEN,
  communQuestionsToAddForBothParcours,
  countQuestionsForParcours,
} from '../../utils/examen-parcours.util';

@Component({
  selector: 'app-examen-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './examen-form.component.html',
})
export class ExamenFormComponent implements OnInit {
  readonly minQuestionsParcours = MIN_QUESTIONS_PAR_EXAMEN;
  formations: Formation[] = [];
  form: ExamenRequest = {
    formationId: 0,
    titre: '',
    description: '',
    seuilReussi: 60,
    questions: [],
  };
  saving = false;
  error = '';

  /** Résultat validateur IA pour la question {@link validationQuestionIndex}. */
  validationResult: QuestionValidationResult | null = null;
  validationQuestionIndex: number | null = null;
  validatingIndex: number | null = null;

  constructor(
    private examenService: ExamenService,
    private formationService: FormationService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.formationService.getAll().subscribe({
      next: (data) => (this.formations = data),
      error: () => this.toast.error('Impossible de charger les formations'),
    });
    const formationId = this.route.snapshot.queryParamMap.get('formationId');
    if (formationId) {
      this.form.formationId = +formationId;
    }
  }

  addQuestion() {
    this.form.questions.push({
      ordre: this.form.questions.length,
      enonce: '',
      optionA: '',
      optionB: '',
      optionC: '',
      optionD: '',
      bonneReponse: 'A',
      parcoursInclusion: 'COMMUN',
      theme: '',
      skill: '',
      explication: '',
    });
  }

  removeQuestion(i: number) {
    this.form.questions.splice(i, 1);
    this.form.questions.forEach((q, idx) => (q.ordre = idx));
    if (this.validationQuestionIndex === i) {
      this.validationResult = null;
      this.validationQuestionIndex = null;
    } else if (this.validationQuestionIndex != null && this.validationQuestionIndex > i) {
      this.validationQuestionIndex--;
    }
  }

  validerQuestionAvecIa(i: number) {
    const q = this.form.questions[i];
    if (!q?.enonce?.trim()) {
      this.toast.error('Renseignez au moins l’énoncé avant la validation IA.');
      return;
    }
    const formation = this.formations.find((f) => f.id === this.form.formationId);
    const ctxParts: string[] = [];
    if (this.form.titre?.trim()) {
      ctxParts.push(`Titre examen : ${this.form.titre.trim()}`);
    }
    if (this.form.description?.trim()) {
      ctxParts.push(`Description examen : ${this.form.description.trim()}`);
    }
    if (formation) {
      ctxParts.push(`Formation : ${formation.titre}`);
      if (formation.description?.trim()) {
        ctxParts.push(formation.description.trim());
      }
    }
    this.validatingIndex = i;
    this.validationQuestionIndex = i;
    this.validationResult = null;
    this.examenService
      .validateQuestionAi({
        enonce: q.enonce,
        optionA: q.optionA,
        optionB: q.optionB,
        optionC: q.optionC,
        optionD: q.optionD,
        bonneReponse: q.bonneReponse,
        theme: q.theme?.trim() || undefined,
        skill: q.skill?.trim() || undefined,
        contexteFormation: ctxParts.length ? ctxParts.join('\n\n') : undefined,
      })
      .subscribe({
        next: (r) => {
          this.validatingIndex = null;
          this.validationResult = r;
          if (!r.llmConfigured) {
            this.toast.error('LLM non configuré. Activez Ollama / app.examen.llm sur le service Evaluation.');
          } else if (!r.parseOk) {
            this.toast.info(r.errorMessage ?? 'Réponse IA non lisible.');
          } else if (r.publishable === false) {
            this.toast.info('L’IA déconseille la publication telle quelle — voir le détail.');
          } else {
            this.toast.success('Validation IA terminée.');
          }
        },
        error: (err) => {
          this.validatingIndex = null;
          this.toast.error(err.error?.message ?? 'Erreur validation IA');
        },
      });
  }

  countStandard(): number {
    return countQuestionsForParcours(this.form.questions, 'STANDARD');
  }

  countRenforcement(): number {
    return countQuestionsForParcours(this.form.questions, 'RENFORCEMENT');
  }

  deficitParcours(): number {
    return communQuestionsToAddForBothParcours(this.form.questions);
  }

  /** Ajoute des lignes QCM en lot Commun pour atteindre le minimum par parcours (Standard et Renforcement). */
  completerAvecQuestionsCommun() {
    const n = this.deficitParcours();
    if (n <= 0) {
      return;
    }
    const base = this.form.questions.length;
    for (let k = 0; k < n; k++) {
      const i = base + k;
      const blank: QuestionDto = {
        ordre: i,
        enonce: '',
        optionA: '',
        optionB: '',
        optionC: '',
        optionD: '',
        bonneReponse: 'A',
        parcoursInclusion: 'COMMUN',
        theme: '',
        skill: '',
        explication: '',
      };
      this.form.questions.push(blank);
    }
    this.form.questions.forEach((q, idx) => (q.ordre = idx));
    this.toast.info(`${n} question(s) ajoutée(s) (lot Commun). Complétez les énoncés et réponses.`);
  }

  onSubmit() {
    if (!this.form.formationId || !this.form.titre?.trim()) {
      this.error = 'Formation et titre requis.';
      return;
    }
    if (this.form.questions.length === 0) {
      this.error = 'Ajoutez au moins une question.';
      return;
    }
    const cStd = this.countStandard();
    const cRen = this.countRenforcement();
    if (cStd < MIN_QUESTIONS_PAR_EXAMEN || cRen < MIN_QUESTIONS_PAR_EXAMEN) {
      this.error =
        `Chaque parcours doit avoir au moins ${MIN_QUESTIONS_PAR_EXAMEN} questions comptées pour le passage : ` +
        `Standard ${cStd}/${MIN_QUESTIONS_PAR_EXAMEN}, Renforcement ${cRen}/${MIN_QUESTIONS_PAR_EXAMEN}. ` +
        `Les questions « Commun » comptent pour les deux. Ajustez les lots ou utilisez « Compléter avec des questions Commun ».`;
      return;
    }
    this.saving = true;
    this.error = '';
    const payload = {
      formationId: Number(this.form.formationId),
      titre: this.form.titre.trim(),
      description: this.form.description?.trim() || '',
      seuilReussi: Number(this.form.seuilReussi) || 60,
      questions: this.form.questions.map((q, i) => ({
        ordre: i,
        enonce: (q.enonce && q.enonce.trim()) ? q.enonce.trim() : 'Question ' + (i + 1),
        optionA: q.optionA?.trim() ?? '',
        optionB: q.optionB?.trim() ?? '',
        optionC: q.optionC?.trim() ?? '',
        optionD: q.optionD?.trim() ?? '',
        bonneReponse: (q.bonneReponse && 'ABCD'.includes(q.bonneReponse.toUpperCase())) ? q.bonneReponse.toUpperCase().substring(0, 1) : 'A',
        parcoursInclusion: (typeof q.parcoursInclusion === 'string' && q.parcoursInclusion) ? q.parcoursInclusion : 'COMMUN',
        theme: q.theme?.trim() ? q.theme.trim() : undefined,
        skill: q.skill?.trim() ? q.skill.trim() : undefined,
        explication: q.explication?.trim() ? q.explication.trim() : undefined,
      })),
    };
    this.examenService.create(payload).subscribe({
      next: (e) => {
        this.toast.success('Examen créé');
        this.router.navigate(['/admin/examens', e.id]);
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.message ?? err.error?.error ?? err.message ?? (err.status === 0 ? 'Serveur inaccessible. Démarrez la Gateway (port 8050) et le microservice Evaluation (port 8083).' : `Erreur ${err.status}`);
        this.error = msg;
        this.toast.error(msg);
      },
    });
  }
}
