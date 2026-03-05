import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormationService } from '../../services/formation.service';
import { ToastService } from '../../services/toast.service';
import {
  FormationRequest,
  NiveauFormation,
  NIVEAU_FORMATION_LABELS,
  StatutFormation,
  TypeFormation,
  TYPE_FORMATION_LABELS,
} from '../../models/formation.model';

@Component({
  selector: 'app-formation-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './formation-form.component.html',
})
export class FormationFormComponent implements OnInit {
  isEdit = false;
  formationId?: number;
  saving = false;
  error = '';

  form: FormationRequest = {
    titre: '',
    typeFormation: 'WEB_DEVELOPMENT',
    description: '',
    dureeHeures: 16,
    dateDebut: '',
    dateFin: '',
    capaciteMax: 20,
    statut: 'OUVERTE',
    niveau: 'DEBUTANT',
  };

  typeFormations: { value: TypeFormation; label: string }[] = Object.entries(TYPE_FORMATION_LABELS).map(
    ([value, label]) => ({ value: value as TypeFormation, label })
  );

  statuts: { value: StatutFormation; label: string }[] = [
    { value: 'OUVERTE', label: 'Ouverte' },
    { value: 'EN_COURS', label: 'En cours' },
    { value: 'TERMINEE', label: 'Terminée' },
    { value: 'ANNULEE', label: 'Annulée' },
  ];

  niveaux: { value: NiveauFormation; label: string }[] = Object.entries(NIVEAU_FORMATION_LABELS).map(
    ([value, label]) => ({ value: value as NiveauFormation, label })
  );

  /** Date minimale pour les champs date (aujourd'hui) — en création, on n'autorise pas les dates passées */
  get minDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  /** En édition, ne pas imposer de date min pour ne pas invalider les formations déjà passées */
  get useMinDate(): boolean {
    return !this.isEdit;
  }

  /** La date de fin doit être >= date de début */
  get datesOrderInvalid(): boolean {
    const d = this.form.dateDebut;
    const f = this.form.dateFin;
    if (!d || !f) return false;
    return f < d;
  }

  /** Date min pour le champ date fin : au moins date de début, et en création au moins aujourd'hui */
  get minDateFin(): string | null {
    const today = this.minDate;
    const d = this.form.dateDebut;
    if (!d) return this.useMinDate ? today : null;
    if (this.useMinDate && d < today) return today;
    return d;
  }

  constructor(
    private formationService: FormationService,
    private toast: ToastService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.formationId = +id;
      this.formationService.getById(this.formationId).subscribe({
        next: (f) => {
          this.form = {
            titre: f.titre,
            typeFormation: f.typeFormation ?? 'WEB_DEVELOPMENT',
            description: f.description ?? '',
            dureeHeures: f.dureeHeures,
            dateDebut: f.dateDebut,
            dateFin: f.dateFin,
            capaciteMax: f.capaciteMax ?? 20,
            statut: f.statut,
            niveau: f.niveau ?? 'DEBUTANT',
          };
        },
        error: () => this.router.navigate(['/admin/formations']),
      });
    } else {
      const today = new Date().toISOString().slice(0, 10);
      this.form.dateDebut = today;
      this.form.dateFin = today;
    }
  }

  onSubmit() {
    this.saving = true;
    this.error = '';

    const obs = this.isEdit
      ? this.formationService.update(this.formationId!, this.form)
      : this.formationService.create(this.form);

    obs.subscribe({
      next: (f) => {
        this.toast.success(this.isEdit ? 'Formation modifiée' : 'Formation créée');
        this.router.navigate(['/admin/formations', f.id]);
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.error ?? 'Erreur. Vérifiez les champs.';
        this.error = msg;
        this.toast.error(msg);
      },
    });
  }
}
