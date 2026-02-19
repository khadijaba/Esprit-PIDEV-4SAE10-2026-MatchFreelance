import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ExamenService } from '../../services/examen.service';
import { FormationService } from '../../services/formation.service';
import { ToastService } from '../../services/toast.service';
import { ExamenRequest } from '../../models/examen.model';
import { Formation } from '../../models/formation.model';

@Component({
  selector: 'app-examen-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './examen-form.component.html',
})
export class ExamenFormComponent implements OnInit {
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
    });
  }

  removeQuestion(i: number) {
    this.form.questions.splice(i, 1);
    this.form.questions.forEach((q, idx) => (q.ordre = idx));
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
      })),
    };
    this.examenService.create(payload).subscribe({
      next: (e) => {
        this.toast.success('Examen créé');
        this.router.navigate(['/admin/examens', e.id]);
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.message ?? err.error?.error ?? err.message ?? (err.status === 0 ? 'Serveur inaccessible. Démarrez la Gateway (port 8082) et le microservice Evaluation (port 8083).' : `Erreur ${err.status}`);
        this.error = msg;
        this.toast.error(msg);
      },
    });
  }
}
