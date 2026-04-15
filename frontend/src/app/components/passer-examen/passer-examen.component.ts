import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { timeout } from 'rxjs/operators';
import { ExamenService } from '../../services/examen.service';
import { ToastService } from '../../services/toast.service';
import { AuthService, FREELANCER_ID_STORAGE_KEY } from '../../services/auth.service';
import {
  Certificat,
  EvaluationRisque,
  Examen,
  ObjectifTheme,
  PassageExamen,
  ProjetMarche,
  QuestionDto,
  RemediationPlan,
  SuccessPrediction,
  TypeParcours,
} from '../../models/examen.model';
import { CertificatDisplayComponent } from '../certificat-display/certificat-display.component';

@Component({
  selector: 'app-passer-examen',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CertificatDisplayComponent],
  templateUrl: './passer-examen.component.html',
})
export class PasserExamenComponent implements OnInit, OnDestroy {
  /** Temps par question (classique et adaptatif), en secondes — peut être majoré (accessibilité). */
  secondesParQuestion = 10;
  formationId = 0;
  private examenId = 0;
  examen: Examen | null = null;
  reponses: string[] = [];
  loading = true;
  submitting = false;
  selectedParcours: TypeParcours = 'STANDARD';
  risque: EvaluationRisque | null = null;
  risqueLoading = false;
  simulation: SuccessPrediction | null = null;
  simulationLoading = false;
  simulationError: string | null = null;
  planRemediation: RemediationPlan | null = null;
  planRemediationLoading = false;
  planRemediationError: string | null = null;
  completedRemediationSteps = new Set<number>();
  resultat: PassageExamen | null = null;
  certificat: Certificat | null = null;
  certificatLoading = false;
  freelancerIdInput = '';

  /** Certifiant : une tentative enregistrée ; entraînement : correction + trace pour révision / objectifs. */
  passageMode: 'CERTIFIANT' | 'ENTRAINEMENT' = 'CERTIFIANT';
  /** Sous-ensemble des questions déjà ratées (entraînement + classique). */
  revisionCiblee = false;

  objectifs: ObjectifTheme[] = [];
  newObjectifTheme = '';
  newObjectifScore = 80;

  /** Mode classique = toutes les questions ; adaptatif = une question à la fois, difficulté ajustée. */
  examMode: 'classique' | 'adaptatif' = 'classique';
  adaptatifToken: string | null = null;
  adaptatifQuestion: QuestionDto | null = null;
  adaptatifNumero = 0;
  adaptatifTotal = 0;
  adaptatifDifficulteLibelle = '';
  adaptatifReponse = '';
  adaptatifDemarrage = false;
  adaptatifEtapeFeedback: { correct: boolean; difficulte?: string } | null = null;
  /** Compte à rebours pour la question adaptative en cours (null = inactif). */
  adaptatifChronoRestant: number | null = null;
  private adaptatifChronoId: ReturnType<typeof setInterval> | null = null;

  /** Mode classique : une question à la fois avec chrono (index dans examen.questions). */
  classiqueQuestionIndex = 0;
  classiqueChronoRestant: number | null = null;
  private classiqueChronoId: ReturnType<typeof setInterval> | null = null;

  constructor(
    private examenService: ExamenService,
    private toast: ToastService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    const fid = this.route.snapshot.paramMap.get('formationId');
    const eid = this.route.snapshot.paramMap.get('examenId');
    if (fid) this.formationId = +fid;
    if (!eid) {
      this.router.navigate(['/formations']);
      return;
    }
    const user = this.auth.getStoredUser();
    if (user?.role === 'FREELANCER' && user.userId != null) {
      this.freelancerIdInput = String(user.userId);
      localStorage.setItem(FREELANCER_ID_STORAGE_KEY, String(user.userId));
    } else {
      const stored = localStorage.getItem(FREELANCER_ID_STORAGE_KEY);
      if (stored) this.freelancerIdInput = stored;
    }

    this.examenId = +eid;
    this.loadExamenPourParcours();
  }

  ngOnDestroy(): void {
    this.arreterChronoAdaptatif();
    this.arreterChronoClassique();
  }

  loadExamenPourParcours() {
    this.loading = true;
    const fid = this.currentFreelancerId;
    if (this.revisionCiblee && fid == null) {
      this.revisionCiblee = false;
      this.toast.info('Connectez-vous ou saisissez votre ID freelancer pour la révision ciblée.');
    }
    const examen$ =
      this.revisionCiblee && fid != null
        ? this.examenService.getPourRevision(this.examenId, fid, this.selectedParcours)
        : this.examenService.getPourPassage(this.examenId, this.selectedParcours);
    examen$.subscribe({
      next: (e) => {
        this.examen = e;
        this.reponses = (e.questions ?? []).map(() => '');
        this.classiqueQuestionIndex = 0;
        this.arreterChronoClassique();
        this.loading = false;
        if (this.revisionCiblee && fid != null && (e.questions?.length ?? 0) === 0) {
          this.toast.info(
            'Aucune question en révision pour l’instant : faites d’abord un entraînement (ou un passage) pour enregistrer des erreurs.'
          );
        }
        const fid2 = this.currentFreelancerId;
        if (fid2 != null) {
          this.refreshRisque();
          this.refreshSimulationEtRemediation();
          this.refreshAmenagementEtObjectifs();
          if (this.passageMode === 'CERTIFIANT') {
            this.examenService.getPassage(this.examenId, fid2).subscribe({
              next: (passage) => {
                this.arreterChronoClassique();
                this.resultat = passage;
                if (passage.resultat === 'REUSSI' && passage.certificat) {
                  this.certificat = passage.certificat;
                } else if (passage.resultat === 'REUSSI' && passage.id != null) {
                  this.loadCertificat(passage.id);
                }
              },
              error: () => {
                this.planifierChronoClassiqueSiBesoin();
              },
            });
          } else {
            this.resultat = null;
            this.certificat = null;
            this.planifierChronoClassiqueSiBesoin();
          }
        } else {
          this.risque = null;
          this.simulation = null;
          this.planRemediation = null;
          this.planifierChronoClassiqueSiBesoin();
        }
      },
      error: () => {
        this.loading = false;
        this.toast.error('Examen introuvable');
        this.router.navigate(['/formations', this.formationId || '']);
      },
    });
  }

  private refreshAmenagementEtObjectifs(): void {
    const fid = this.currentFreelancerId;
    if (fid == null) {
      return;
    }
    this.examenService.getAmenagementTemps(fid).subscribe({
      next: (a) => {
        this.secondesParQuestion = Math.max(5, a.secondesEffectivesParQuestion ?? 10);
      },
      error: () => {
        this.secondesParQuestion = 10;
      },
    });
    this.examenService.listObjectifsTheme(fid).subscribe({
      next: (o) => (this.objectifs = o),
      error: () => (this.objectifs = []),
    });
  }

  onPassageModeChange(): void {
    this.resultat = null;
    this.certificat = null;
    if (this.passageMode === 'CERTIFIANT') {
      this.revisionCiblee = false;
    }
    this.resetAdaptatifSession();
    this.classiqueQuestionIndex = 0;
    this.arreterChronoClassique();
    this.loadExamenPourParcours();
  }

  onRevisionCibleeChange(): void {
    if (this.revisionCiblee) {
      this.passageMode = 'ENTRAINEMENT';
    }
    this.resetAdaptatifSession();
    this.classiqueQuestionIndex = 0;
    this.arreterChronoClassique();
    this.loadExamenPourParcours();
  }

  creerObjectif(): void {
    const fid = this.currentFreelancerId;
    const theme = this.newObjectifTheme.trim();
    if (!fid || !this.examen?.id || !theme) {
      this.toast.error('Thème et ID freelancer requis.');
      return;
    }
    this.examenService
      .creerObjectifTheme(fid, {
        examenId: this.examen.id,
        theme,
        objectifScore: this.newObjectifScore,
      })
      .subscribe({
        next: () => {
          this.newObjectifTheme = '';
          this.refreshAmenagementEtObjectifs();
          this.toast.success('Objectif enregistré.');
        },
        error: (err) => this.toast.error(err.error?.message ?? 'Erreur'),
      });
  }

  supprimerObjectif(id: number): void {
    const fid = this.currentFreelancerId;
    if (!fid) return;
    this.examenService.supprimerObjectifTheme(fid, id).subscribe({
      next: () => {
        this.refreshAmenagementEtObjectifs();
        this.toast.success('Objectif supprimé.');
      },
      error: () => this.toast.error('Suppression impossible'),
    });
  }

  onParcoursChange() {
    this.resultat = null;
    this.certificat = null;
    this.resetAdaptatifSession();
    this.loadExamenPourParcours();
  }

  onExamModeChange() {
    if (this.examMode === 'adaptatif') {
      this.revisionCiblee = false;
    }
    this.resetAdaptatifSession();
    this.arreterChronoClassique();
    this.classiqueQuestionIndex = 0;
    this.resultat = null;
    this.certificat = null;
    this.planifierChronoClassiqueSiBesoin();
  }

  private resetAdaptatifSession() {
    this.arreterChronoAdaptatif();
    this.adaptatifToken = null;
    this.adaptatifQuestion = null;
    this.adaptatifNumero = 0;
    this.adaptatifTotal = 0;
    this.adaptatifDifficulteLibelle = '';
    this.adaptatifReponse = '';
    this.adaptatifDemarrage = false;
    this.adaptatifEtapeFeedback = null;
  }

  demarrerAdaptatif() {
    const fid = this.currentFreelancerId;
    if (!fid || !this.examen?.id) return;
    this.adaptatifDemarrage = true;
    this.adaptatifEtapeFeedback = null;
    this.examenService
      .demarrerAdaptatif(this.examen.id, {
        freelancerId: fid,
        typeParcours: this.selectedParcours,
        mode: 'CERTIFIANT',
      })
      .subscribe({
        next: (d) => {
          this.adaptatifDemarrage = false;
          this.adaptatifToken = d.token;
          this.adaptatifQuestion = d.question;
          this.adaptatifNumero = d.numeroQuestion;
          this.adaptatifTotal = d.questionsTotal;
          this.adaptatifDifficulteLibelle = d.difficulteCible ?? '';
          this.adaptatifReponse = '';
          this.demarrerChronoAdaptatif();
          this.toast.success('Examen adaptatif démarré. Répondez question par question.');
        },
        error: (err) => {
          this.adaptatifDemarrage = false;
          this.toast.error(err.error?.message ?? 'Impossible de démarrer l’examen adaptatif');
        },
      });
  }

  canDemarrerAdaptatif(): boolean {
    return (
      this.examMode === 'adaptatif' &&
      !!this.currentFreelancerId &&
      !!this.examen?.id &&
      !this.adaptatifToken &&
      !this.adaptatifDemarrage
    );
  }

  canSubmitAdaptatif(): boolean {
    return (
      !!this.adaptatifToken &&
      this.adaptatifQuestion?.id != null &&
      !!this.adaptatifReponse &&
      !this.submitting
    );
  }

  private demarrerChronoAdaptatif(): void {
    this.arreterChronoAdaptatif();
    if (!this.adaptatifToken || !this.adaptatifQuestion?.id) {
      return;
    }
    this.adaptatifChronoRestant = this.secondesPourQuestion(this.adaptatifQuestion.niveauDifficulte);
    this.adaptatifChronoId = setInterval(() => {
      if (this.adaptatifChronoRestant == null) {
        return;
      }
      this.adaptatifChronoRestant--;
      if (this.adaptatifChronoRestant <= 0) {
        this.arreterChronoAdaptatif();
        if (!this.submitting && this.adaptatifToken && this.adaptatifQuestion?.id != null) {
          this.submitAdaptatif(true);
        }
      }
    }, 1000);
  }

  private arreterChronoAdaptatif(): void {
    if (this.adaptatifChronoId != null) {
      clearInterval(this.adaptatifChronoId);
      this.adaptatifChronoId = null;
    }
    this.adaptatifChronoRestant = null;
  }

  /**
   * @param tempsEcoule si vrai : envoi automatique à0 s (réponse « ? » si aucun choix).
   */
  submitAdaptatif(tempsEcoule = false) {
    const fid = this.currentFreelancerId;
    if (!fid || !this.examen?.id || !this.adaptatifToken || this.adaptatifQuestion?.id == null) return;
    if (this.submitting) {
      return;
    }
    this.arreterChronoAdaptatif();

    let rep = (this.adaptatifReponse ?? '').toUpperCase().trim();
    if (!rep) {
      rep = tempsEcoule ? '?' : 'A';
    } else {
      rep = rep.substring(0, 1);
    }
    this.submitting = true;
    this.examenService
      .repondreAdaptatif(this.examen.id, this.adaptatifToken, {
        questionId: this.adaptatifQuestion.id,
        reponse: rep,
      })
      .subscribe({
        next: (step) => {
          this.submitting = false;
          if (step.termine && step.resultat) {
            this.arreterChronoAdaptatif();
            this.resultat = step.resultat;
            this.adaptatifToken = null;
            this.adaptatifQuestion = null;
            if (step.resultat.freelancerId) {
              localStorage.setItem(FREELANCER_ID_STORAGE_KEY, String(step.resultat.freelancerId));
            }
            this.toast.success(
              step.resultat.resultat === 'REUSSI' ? 'Examen réussi ! Certificat délivré.' : 'Examen terminé.'
            );
            if (step.resultat.resultat === 'REUSSI') {
              if (step.resultat.certificat) {
                this.certificat = step.resultat.certificat;
              } else if (step.resultat.id != null) {
                this.loadCertificat(step.resultat.id);
              }
              this.mergeCarriereDepuisServeur();
            }
            return;
          }
          this.adaptatifEtapeFeedback = {
            correct: step.reponseCorrecte,
            difficulte: step.difficulteApresAjustement,
          };
          if (step.prochaineQuestion) {
            this.adaptatifQuestion = step.prochaineQuestion;
            this.adaptatifReponse = '';
            if (step.numeroQuestion != null) this.adaptatifNumero = step.numeroQuestion;
            if (step.questionsTotal != null) this.adaptatifTotal = step.questionsTotal;
            if (step.difficulteApresAjustement) {
              this.adaptatifDifficulteLibelle = step.difficulteApresAjustement;
            }
            this.demarrerChronoAdaptatif();
          }
        },
        error: (err) => {
          this.submitting = false;
          if (this.adaptatifToken && this.adaptatifQuestion?.id) {
            this.demarrerChronoAdaptatif();
          }
          this.toast.error(err.error?.message ?? 'Erreur lors de l’envoi de la réponse');
        },
      });
  }

  refreshRisque() {
    const fid = this.currentFreelancerId;
    if (!fid) {
      this.risque = null;
      return;
    }
    this.risqueLoading = true;
    this.examenService.getEvaluationRisque(this.examenId, fid).subscribe({
      next: (r) => {
        this.risque = r;
        this.risqueLoading = false;
      },
      error: () => {
        this.risque = null;
        this.risqueLoading = false;
      },
    });
  }

  refreshSimulationEtRemediation(): void {
    const fid = this.currentFreelancerId;
    if (!fid || !this.examen?.id) {
      this.simulation = null;
      this.planRemediation = null;
      this.simulationError = 'Renseignez un freelancer ID valide pour lancer la simulation.';
      this.planRemediationError = 'Renseignez un freelancer ID valide pour générer le learning path.';
      return;
    }
    this.simulationLoading = true;
    this.planRemediationLoading = true;
    this.simulationError = null;
    this.planRemediationError = null;
    this.examenService.getSimulationReussite(this.examen.id, fid).pipe(timeout(10000)).subscribe({
      next: (s) => {
        this.simulation = s;
        this.simulationLoading = false;
      },
      error: (err) => {
        this.simulation = null;
        this.simulationLoading = false;
        const msg = err?.name === 'TimeoutError'
          ? 'Simulation : délai dépassé (10s). Vérifiez Gateway 8050 / Evaluation 8083.'
          : err?.error?.message ?? `Simulation indisponible (HTTP ${err?.status ?? 'N/A'})`;
        this.simulationError = msg;
      },
    });
    this.examenService.getPlanRemediation(this.examen.id, fid).pipe(timeout(10000)).subscribe({
      next: (p) => {
        this.planRemediation = p;
        this.hydraterEtapesRemediationDepuisStockage();
        this.planRemediationLoading = false;
      },
      error: (err) => {
        this.planRemediation = null;
        this.planRemediationLoading = false;
        const msg = err?.name === 'TimeoutError'
          ? 'Plan de remédiation : délai dépassé (10s). Vérifiez Gateway 8050 / Evaluation 8083.'
          : err?.error?.message ?? `Plan de remédiation indisponible (HTTP ${err?.status ?? 'N/A'})`;
        this.planRemediationError = msg;
      },
    });
  }

  private remediationStorageKey(): string | null {
    if (!this.examen?.id || !this.currentFreelancerId) {
      return null;
    }
    return `remediation:${this.currentFreelancerId}:${this.examen.id}`;
  }

  private hydraterEtapesRemediationDepuisStockage(): void {
    this.completedRemediationSteps.clear();
    const key = this.remediationStorageKey();
    if (!key) {
      return;
    }
    try {
      const raw = localStorage.getItem(key);
      if (!raw) {
        return;
      }
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        parsed.forEach((n) => {
          const v = Number(n);
          if (!Number.isNaN(v)) {
            this.completedRemediationSteps.add(v);
          }
        });
      }
    } catch {
      // ignore localStorage parse issues
    }
  }

  private sauvegarderEtapesRemediation(): void {
    const key = this.remediationStorageKey();
    if (!key) {
      return;
    }
    localStorage.setItem(key, JSON.stringify(Array.from(this.completedRemediationSteps.values())));
  }

  isEtapeRemediationTerminee(sequence?: number): boolean {
    return sequence != null ? this.completedRemediationSteps.has(sequence) : false;
  }

  toggleEtapeRemediation(sequence?: number): void {
    if (sequence == null) {
      return;
    }
    if (this.completedRemediationSteps.has(sequence)) {
      this.completedRemediationSteps.delete(sequence);
    } else {
      this.completedRemediationSteps.add(sequence);
    }
    this.sauvegarderEtapesRemediation();
  }

  onFreelancerIdChanged() {
    this.resultat = null;
    this.certificat = null;
    this.resetAdaptatifSession();
    this.classiqueQuestionIndex = 0;
    this.arreterChronoClassique();
    this.refreshRisque();
    this.refreshSimulationEtRemediation();
    this.refreshAmenagementEtObjectifs();
    const fid = this.currentFreelancerId;
    if (fid != null && this.passageMode === 'CERTIFIANT') {
      this.examenService.getPassage(this.examenId, fid).subscribe({
        next: (passage) => {
          this.arreterChronoClassique();
          this.resultat = passage;
          if (passage.resultat === 'REUSSI' && passage.certificat) {
            this.certificat = passage.certificat;
          } else if (passage.resultat === 'REUSSI' && passage.id != null) {
            this.loadCertificat(passage.id);
          }
        },
        error: () => {
          this.resultat = null;
          this.planifierChronoClassiqueSiBesoin();
        },
      });
    } else {
      this.planifierChronoClassiqueSiBesoin();
    }
    if (this.revisionCiblee) {
      this.loadExamenPourParcours();
    }
  }

  get currentFreelancerId(): number | null {
    const u = this.auth.getStoredUser();
    if (u?.role === 'FREELANCER' && u.userId != null) {
      return u.userId;
    }
    const n = parseInt(this.freelancerIdInput, 10);
    return Number.isNaN(n) ? null : n;
  }

  get isFreelancerConnected(): boolean {
    const u = this.auth.getStoredUser();
    return u?.role === 'FREELANCER' && !!u.userId;
  }

  get connectedFreelancerId(): number | null {
    const u = this.auth.getStoredUser();
    return u?.role === 'FREELANCER' && u.userId != null ? u.userId : null;
  }

  canSubmit(): boolean {
    if (!this.examen?.questions?.length || !this.currentFreelancerId) return false;
    return !this.submitting;
  }

  /** Question affichée en mode classique (une par une). */
  get classiqueQuestionCourante(): QuestionDto | null {
    const qs = this.examen?.questions;
    if (!qs?.length) return null;
    const i = this.classiqueQuestionIndex;
    return i >= 0 && i < qs.length ? qs[i] : null;
  }

  private planifierChronoClassiqueSiBesoin(): void {
    setTimeout(() => {
      if (
        this.examMode === 'classique' &&
        this.examen?.questions?.length &&
        !this.resultat &&
        !this.loading
      ) {
        this.demarrerChronoClassique();
      }
    }, 0);
  }

  private demarrerChronoClassique(): void {
    this.arreterChronoClassique();
    if (
      this.examMode !== 'classique' ||
      !this.examen?.questions?.length ||
      this.resultat ||
      this.loading
    ) {
      return;
    }
    this.classiqueChronoRestant = this.secondesPourQuestion(this.classiqueQuestionCourante?.niveauDifficulte);
    this.classiqueChronoId = setInterval(() => {
      if (this.classiqueChronoRestant == null) return;
      this.classiqueChronoRestant--;
      if (this.classiqueChronoRestant <= 0) {
        this.arreterChronoClassique();
        if (!this.submitting && !this.resultat && this.examen?.questions?.length) {
          this.avancerClassique(true);
        }
      }
    }, 1000);
  }

  private arreterChronoClassique(): void {
    if (this.classiqueChronoId != null) {
      clearInterval(this.classiqueChronoId);
      this.classiqueChronoId = null;
    }
    this.classiqueChronoRestant = null;
  }

  /**
   * Mode classique : passer à la question suivante ou envoyer l’examen.
   * @param tempsEcoule true si déclenché par le chrono (pas de réponse = traitée comme non répondue).
   */
  avancerClassique(tempsEcoule = false): void {
    const qs = this.examen?.questions ?? [];
    if (!this.examen || qs.length === 0 || this.submitting || this.resultat) return;
    this.arreterChronoClassique();
    const i = this.classiqueQuestionIndex;
    if (tempsEcoule && !(this.reponses[i] ?? '').trim()) {
      this.reponses[i] = '';
    }
    if (i >= qs.length - 1) {
      this.submit();
      return;
    }
    this.classiqueQuestionIndex++;
    this.demarrerChronoClassique();
  }

  submit() {
    const fid = this.currentFreelancerId;
    if (!fid || !this.examen) return;
    this.arreterChronoClassique();
    const rep = this.reponses.map((r) => {
      const x = (r ?? '').toUpperCase().trim();
      const lettre = x.length > 0 ? x.substring(0, 1) : '';
      return ['A', 'B', 'C', 'D'].includes(lettre) ? lettre : '?';
    });
    this.submitting = true;
    this.examenService
      .passerExamen(this.examen.id, {
        freelancerId: fid,
        reponses: rep,
        typeParcours: this.selectedParcours,
        mode: this.passageMode,
        revisionCiblee:
          this.passageMode === 'ENTRAINEMENT' && this.revisionCiblee && this.examMode === 'classique',
      })
      .subscribe({
      next: (p) => {
        this.resultat = p;
        this.submitting = false;
        if (p.freelancerId) localStorage.setItem(FREELANCER_ID_STORAGE_KEY, String(p.freelancerId));
        if (this.passageMode === 'CERTIFIANT') {
          this.toast.success(p.resultat === 'REUSSI' ? 'Examen réussi ! Certificat délivré.' : 'Examen terminé.');
        } else {
          this.toast.success('Entraînement terminé. Consultez la correction et vos scores par thème ci-dessous.');
        }
        if (p.resultat === 'REUSSI' && this.passageMode === 'CERTIFIANT') {
          if (p.certificat) {
            this.certificat = p.certificat;
          } else if (p.id != null) {
            this.loadCertificat(p.id);
          }
          this.mergeCarriereDepuisServeur();
        }
        this.refreshAmenagementEtObjectifs();
      },
      error: (err) => {
        this.submitting = false;
        if (
          this.examMode === 'classique' &&
          this.examen?.questions?.length &&
          this.classiqueQuestionIndex === this.examen.questions.length - 1
        ) {
          this.demarrerChronoClassique();
        }
        this.toast.error(err.error?.message ?? 'Erreur lors de la soumission');
      },
    });
  }

  trackByIndex(i: number) {
    return i;
  }

  /** Affiche le score d’adéquation compétences (0–100) ou « — » si l’API ne l’a pas fourni. */
  projetScoreAffiche(proj: ProjetMarche): number | string {
    const s = proj.scoreAlignementSkills;
    return s != null && !Number.isNaN(Number(s)) ? s : '—';
  }

  /**
   * Pourcentage pondéré brut : (points obtenus / points max) × 100, avant tout arrondi d’affichage éventuel côté API.
   */
  pourcentageBrutNote(): number | null {
    const r = this.resultat;
    if (!r || r.pointsObtenus == null || r.pointsMax == null || r.pointsMax <= 0) {
      return null;
    }
    return (r.pointsObtenus / r.pointsMax) * 100;
  }

  /**
   * Affichage à côté du chrono : niveau + poids (FACILE 1, MOYEN 2, DIFFICILE 3), comme en correction.
   */
  /** Poids FACILE 1 / MOYEN 2 / DIFFICILE 3 (aligné backend). */
  private poidsNiveauQuestion(niveau?: string | null): number {
    const n = (niveau ?? 'MOYEN').toUpperCase();
    if (n === 'FACILE') return 1;
    if (n === 'DIFFICILE') return 3;
    return 2;
  }

  /**
   * Chrono par difficulté : FACILE = 50% du temps de base, MOYEN = 100%, DIFFICILE = 150%.
   * Le temps de base peut déjà être majoré par aménagement d'accessibilité.
   */
  private secondesPourQuestion(niveau?: string | null): number {
    const poids = this.poidsNiveauQuestion(niveau); // 1, 2, 3
    const base = Math.max(5, this.secondesParQuestion);
    return Math.max(5, Math.round((base * poids) / 2));
  }

  /** Temps alloué à la question classique courante, après pondération de difficulté. */
  get classiqueSecondesQuestion(): number {
    return this.secondesPourQuestion(this.classiqueQuestionCourante?.niveauDifficulte);
  }

  /** Temps alloué à la question adaptative courante, après pondération de difficulté. */
  get adaptatifSecondesQuestion(): number {
    return this.secondesPourQuestion(this.adaptatifQuestion?.niveauDifficulte);
  }

  /** Aide affichage template (Angular n'expose pas Math directement). */
  get secondesFacileEstimees(): number {
    return this.secondesPourQuestion('FACILE');
  }

  /** Aide affichage template (Angular n'expose pas Math directement). */
  get secondesDifficileEstimees(): number {
    return this.secondesPourQuestion('DIFFICILE');
  }

  /**
   * Avant l’examen : compétences couvertes par les questions du parcours chargé (skill, sinon thème).
   */
  repartitionSkillsPourExamen(): { label: string; count: number; poidsMax: number }[] {
    const qs = this.examen?.questions ?? [];
    const map = new Map<string, { count: number; poidsMax: number }>();
    for (const q of qs) {
      const raw = (q.skill?.trim() || q.theme?.trim() || 'Autres').trim() || 'Autres';
      const p = this.poidsNiveauQuestion(q.niveauDifficulte);
      const cur = map.get(raw) ?? { count: 0, poidsMax: 0 };
      cur.count += 1;
      cur.poidsMax += p;
      map.set(raw, cur);
    }
    return Array.from(map.entries())
      .map(([label, v]) => ({ label, ...v }))
      .sort((a, b) => a.label.localeCompare(b.label, 'fr'));
  }

  libelleSkillQuestion(q: { skill?: string | null; theme?: string | null } | null): string | null {
    if (!q) return null;
    const s = (q.skill?.trim() || q.theme?.trim() || '').trim();
    return s.length ? s : null;
  }

  libelleComplexiteQuestion(niveau: string | null | undefined): string | null {
    if (niveau == null || !String(niveau).trim()) {
      return null;
    }
    const raw = String(niveau).trim();
    const n = raw.toUpperCase();
    let poids: number | null = null;
    if (n === 'FACILE') {
      poids = 1;
    } else if (n === 'MOYEN') {
      poids = 2;
    } else if (n === 'DIFFICILE') {
      poids = 3;
    }
    return poids != null ? `${n} · poids ${poids}` : raw;
  }

  private loadCertificat(passageId: number) {
    this.certificatLoading = true;
    this.examenService.getCertificatByPassage(passageId).subscribe({
      next: (c) => {
        this.certificat = c;
        this.certificatLoading = false;
        this.mergeCarriereDepuisServeur();
      },
      error: () => {
        this.certificatLoading = false;
      },
    });
  }

  /** Complète resultat avec GET /examens/{id}/freelancer/{id} (niveau, compétences, projets). */
  private mergeCarriereDepuisServeur(): void {
    const fid = this.currentFreelancerId;
    if (!this.examen?.id || fid == null || !this.resultat) return;
    this.examenService.getPassage(this.examen.id, fid).subscribe({
      next: (p) => {
        this.resultat = {
          ...this.resultat!,
          niveauCalcule: p.niveauCalcule ?? this.resultat!.niveauCalcule,
          messageCarriere: p.messageCarriere ?? this.resultat!.messageCarriere,
          competencesAttribuees: p.competencesAttribuees ?? this.resultat!.competencesAttribuees,
          projetsMarcheRecommandes: p.projetsMarcheRecommandes ?? this.resultat!.projetsMarcheRecommandes,
          certificat: p.certificat ?? this.resultat!.certificat,
        };
        if (p.certificat && !this.certificat) {
          this.certificat = p.certificat;
        }
      },
      error: () => {},
    });
  }

  /** Affichage : niveau renvoyé par l’API ou dérivé du score (même règles que le backend). */
  niveauCarriereAffiche(): string {
    const r = this.resultat;
    if (!r) return '';
    if (r.niveauCalcule) return r.niveauCalcule;
    return this.niveauFromScore(r.score, this.examen?.seuilReussi ?? 60);
  }

  messageCarriereAffiche(): string {
    return (
      this.resultat?.messageCarriere ??
      'Votre niveau est déduit du score. Si les compétences ou projets n’apparaissent pas, vérifiez que Skill, Project et Eureka tournent, puis rechargez la page.'
    );
  }

  private niveauFromScore(score: number, seuil: number): string {
    if (score >= 95) return 'EXPERT';
    if (score >= 85) return 'AVANCE';
    const interSup = Math.max(seuil + 15, 75);
    if (score >= interSup) return 'INTERMEDIAIRE_SUPERIEUR';
    return 'INTERMEDIAIRE';
  }

  /** Libellé français pour l’affichage du niveau métier. */
  niveauCarriereLibelle(): string {
    const code = this.niveauCarriereAffiche();
    const lib: Record<string, string> = {
      EXPERT: 'Expert',
      AVANCE: 'Avancé',
      INTERMEDIAIRE_SUPERIEUR: 'Intermédiaire supérieur',
      INTERMEDIAIRE: 'Intermédiaire',
    };
    return lib[code] ?? code.replace(/_/g, ' ');
  }

  competenceStatutLibelle(statut?: string): string {
    if (!statut) return '';
    const lib: Record<string, string> = {
      CREE: 'Ajoutée au profil',
      DEJA_PRESENTE: 'Déjà enregistrée',
      INDISPONIBLE: 'Non synchronisée',
      SERVICE_INDISPONIBLE: 'Service momentanément indisponible',
    };
    return lib[statut] ?? statut.replace(/_/g, ' ');
  }

  competenceStatutBadgeClass(statut?: string): string[] {
    switch (statut) {
      case 'CREE':
        return ['bg-emerald-100', 'text-emerald-800', 'ring-emerald-600/20'];
      case 'DEJA_PRESENTE':
        return ['bg-amber-100', 'text-amber-900', 'ring-amber-600/20'];
      case 'SERVICE_INDISPONIBLE':
      case 'INDISPONIBLE':
        return ['bg-slate-200', 'text-slate-800', 'ring-slate-500/15'];
      default:
        return ['bg-slate-100', 'text-slate-700', 'ring-slate-500/10'];
    }
  }

  domaineLibelle(code?: string): string {
    if (!code || code === '—') return '—';
    return code.replace(/_/g, ' ');
  }

  /** Libellé français pour le code niveau stocké sur une compétence. */
  niveauCodeLibelle(code?: string | null): string {
    if (!code) return '—';
    const lib: Record<string, string> = {
      EXPERT: 'Expert',
      AVANCE: 'Avancé',
      INTERMEDIAIRE_SUPERIEUR: 'Intermédiaire supérieur',
      INTERMEDIAIRE: 'Intermédiaire',
    };
    return lib[code] ?? code.replace(/_/g, ' ');
  }

  openCertificatPdf() {
    if (!this.certificat?.id) return;
    this.examenService.getCertificatPdf(this.certificat.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener');
      },
      error: () => {},
    });
  }

  /** Ouvre la page certificat en lui passant les données pour éviter « Certificat introuvable ». */
  goToCertificatView() {
    if (!this.certificat?.id) return;
    this.router.navigate(['/certificat', this.certificat.id], { state: { certificat: this.certificat } });
  }
}
