import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { ExamenService } from '../../services/examen.service';
import { EvaluationReportsService } from '../../services/evaluation-reports.service';
import { Examen, PassageExamen } from '../../models/examen.model';

@Component({
  selector: 'app-rapports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rapports.component.html',
})
export class RapportsComponent implements OnInit, OnDestroy {
  private readonly examenService = inject(ExamenService);
  private readonly reports = inject(EvaluationReportsService);
  private readonly sanitizer = inject(DomSanitizer);

  imgInscriptionsError = false;
  imgCertificatsError = false;
  private readonly cacheBuster = Date.now();

  /** --- Rapports évaluation (Python) --- */
  examens: Examen[] = [];
  examensLoading = false;
  examensError: string | null = null;

  selectedExamenId: number | null = null;
  /** Nombre de passages pour l’examen sélectionné (après chargement). */
  passageCount: number | null = null;
  passagesLoading = false;

  loadingPython = false;
  infoPython: string | null = null;
  errorPython: string | null = null;
  /** Aucun passage réel : affiche carte vide + bouton démo. */
  showEmptyPassages = false;
  /** Les graphiques affichés viennent des données de démonstration. */
  isDemoPreview = false;

  histoUrl: SafeResourceUrl | null = null;
  evolutionUrl: SafeResourceUrl | null = null;
  private objectUrlHisto: string | null = null;
  private objectUrlEvo: string | null = null;

  get graphInscriptionsSrc(): string {
    return `/reports/graph_inscriptions_par_formation.png?t=${this.cacheBuster}`;
  }
  get graphCertificatsSrc(): string {
    return `/reports/graph_certificats_par_mois.png?t=${this.cacheBuster}`;
  }

  ngOnInit(): void {
    this.examensLoading = true;
    this.examenService.getAll().subscribe({
      next: (list) => {
        this.examens = list ?? [];
        this.examensLoading = false;
        if (this.examens.length === 0) {
          this.examensError = 'Aucun examen dans la plateforme. Créez-en un depuis Examens.';
        }
      },
      error: () => {
        this.examensLoading = false;
        this.examensError = 'Impossible de charger la liste des examens (Gateway / Evaluation).';
      },
    });
  }

  ngOnDestroy(): void {
    this.revokeBlobUrls();
  }

  private revokeBlobUrls(): void {
    if (this.objectUrlHisto) {
      URL.revokeObjectURL(this.objectUrlHisto);
      this.objectUrlHisto = null;
    }
    if (this.objectUrlEvo) {
      URL.revokeObjectURL(this.objectUrlEvo);
      this.objectUrlEvo = null;
    }
  }

  /** Quand l’admin change d’examen : recharge le nombre de passages, réinitialise l’affichage. */
  onExamenSelectionChange(): void {
    this.errorPython = null;
    this.infoPython = null;
    this.showEmptyPassages = false;
    this.isDemoPreview = false;
    this.revokeBlobUrls();
    this.histoUrl = null;
    this.evolutionUrl = null;
    this.passageCount = null;

    const id = this.selectedExamenId;
    if (!id) return;

    this.passagesLoading = true;
    this.examenService.getResultatsByExamen(id).subscribe({
      next: (passages) => {
        this.passageCount = passages.length;
        this.passagesLoading = false;
        if (passages.length === 0) {
          this.infoPython =
            'Cet examen n’a encore aucun passage. Utilisez l’aperçu démonstration ou attendez des résultats.';
        } else {
          this.infoPython = `${passages.length} passage(s) — cliquez sur « Générer les graphiques ».`;
        }
      },
      error: () => {
        this.passagesLoading = false;
        this.passageCount = null;
        this.errorPython = 'Impossible de lire les résultats pour cet examen.';
      },
    });
  }

  /** Données factices cohérentes pour démo plateforme (toujours fonctionnel sans BDD). */
  private static readonly DEMO_SCORES = [62, 71, 68, 85, 74, 92, 78, 88, 73, 81];
  private static readonly DEMO_LABELS = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Août'];
  private static readonly DEMO_VALUES = [62, 65, 68, 72, 74, 78, 82, 85];

  apercuDemonstration(): void {
    this.errorPython = null;
    this.showEmptyPassages = false;
    this.isDemoPreview = true;
    this.infoPython = 'Aperçu avec données de démonstration (non liées à un examen réel).';
    this.loadingPython = true;
    this.revokeBlobUrls();
    this.histoUrl = null;
    this.evolutionUrl = null;
    this.genererGraphiquesDepuisDonnees(
      RapportsComponent.DEMO_SCORES,
      RapportsComponent.DEMO_LABELS,
      RapportsComponent.DEMO_VALUES,
      'Démonstration — répartition des scores',
      'Démonstration — évolution (exemple)'
    );
  }

  genererRapportsEvaluation(): void {
    const id = this.selectedExamenId;
    if (!id) {
      this.errorPython = 'Sélectionnez un examen dans la liste.';
      return;
    }
    this.errorPython = null;
    this.infoPython = null;
    this.isDemoPreview = false;
    this.showEmptyPassages = false;
    this.loadingPython = true;
    this.revokeBlobUrls();
    this.histoUrl = null;
    this.evolutionUrl = null;

    this.examenService.getResultatsByExamen(id).subscribe({
      next: (passages: PassageExamen[]) => {
        if (!passages.length) {
          this.loadingPython = false;
          this.showEmptyPassages = true;
          this.infoPython = null;
          this.errorPython = null;
          return;
        }
        const scores = passages.map((p) => p.score);
        const sorted = [...passages].sort(
          (a, b) => new Date(a.datePassage).getTime() - new Date(b.datePassage).getTime()
        );
        const labels = sorted.map((p) =>
          new Date(p.datePassage).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' })
        );
        const values = sorted.map((p) => p.score);
        this.genererGraphiquesDepuisDonnees(
          scores,
          labels,
          values,
          `Répartition des notes — examen #${id}`,
          `Évolution des scores — examen #${id}`
        );
      },
      error: (err) => {
        this.loadingPython = false;
        this.errorPython =
          err.status === 404
            ? 'Examen introuvable.'
            : err.status === 0
              ? 'Vérifiez la Gateway (8050) et le microservice Evaluation.'
              : `Erreur : ${err.status ?? ''}`;
      },
    });
  }

  private genererGraphiquesDepuisDonnees(
    scores: number[],
    labels: string[],
    values: number[],
    titreHisto: string,
    titreEvo: string
  ): void {
    forkJoin([
      this.reports.histogram({ scores, title: titreHisto }),
      this.reports.evolution({ labels, values, title: titreEvo }),
    ])
      .pipe(finalize(() => (this.loadingPython = false)))
      .subscribe({
        next: ([blobH, blobE]) => {
          this.objectUrlHisto = URL.createObjectURL(blobH);
          this.histoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrlHisto);
          this.objectUrlEvo = URL.createObjectURL(blobE);
          this.evolutionUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.objectUrlEvo);
          if (!this.isDemoPreview) {
            this.infoPython = 'Graphiques générés. Vous pouvez exporter le PDF.';
          }
        },
        error: (err) => {
          this.errorPython =
            err.status === 0
              ? 'Service Python indisponible. Lancez evaluation-reports-py sur le port 8090 et la Gateway sur 8050.'
              : `Service graphiques : erreur ${err.status ?? ''}`;
        },
      });
  }

  telechargerPdfEvaluation(): void {
    this.errorPython = null;

    if (this.isDemoPreview) {
      this.reports
        .pdfReport({
          title: 'Rapport d’évaluation — démonstration',
          subtitle: 'Données fictives — MatchFreelance (API Python)',
          scores: RapportsComponent.DEMO_SCORES,
          evolution_labels: RapportsComponent.DEMO_LABELS,
          evolution_values: RapportsComponent.DEMO_VALUES,
        })
        .subscribe({
          next: (blob) => this.declencherTelechargement(blob, 'rapport-evaluation-demonstration.pdf'),
          error: (err) => {
            this.errorPython =
              err.status === 0 ? 'Lancez le service Python (8090) et la Gateway (8050).' : `PDF : ${err.status}`;
          },
        });
      return;
    }

    const id = this.selectedExamenId;
    if (!id) {
      this.errorPython = 'Sélectionnez un examen ou utilisez l’aperçu démonstration.';
      return;
    }

    this.examenService.getResultatsByExamen(id).subscribe({
      next: (passages: PassageExamen[]) => {
        if (!passages.length) {
          this.errorPython =
            'Aucun passage pour cet examen. Générez d’abord l’aperçu démo ou attendez des résultats réels.';
          return;
        }
        const scores = passages.map((p) => p.score);
        const sorted = [...passages].sort(
          (a, b) => new Date(a.datePassage).getTime() - new Date(b.datePassage).getTime()
        );
        const evolution_labels = sorted.map((p) =>
          new Date(p.datePassage).toLocaleDateString('fr-FR', {
            day: '2-digit',
            month: 'short',
            year: '2-digit',
          })
        );
        const evolution_values = sorted.map((p) => p.score);

        this.reports
          .pdfReport({
            title: `Rapport d'évaluation — examen #${id}`,
            subtitle: 'Export MatchFreelance (microservice Python)',
            scores,
            evolution_labels,
            evolution_values,
          })
          .subscribe({
            next: (blob) => this.declencherTelechargement(blob, `rapport-evaluation-examen-${id}.pdf`),
            error: (err) => {
              this.errorPython =
                err.status === 0 ? 'Service Python ou Gateway indisponible.' : `PDF : ${err.status}`;
            },
          });
      },
      error: (err) => {
        this.errorPython = err.status === 404 ? 'Examen introuvable.' : 'Erreur lors du chargement des résultats.';
      },
    });
  }

  private declencherTelechargement(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    this.infoPython = `Fichier « ${filename} » téléchargé.`;
  }

  libelleExamen(ex: Examen): string {
    return `${ex.titre} · #${ex.id}`;
  }
}
