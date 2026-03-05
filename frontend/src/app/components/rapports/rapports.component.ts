import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-rapports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rapports.component.html',
})
export class RapportsComponent {
  imgInscriptionsError = false;
  imgCertificatsError = false;
  /** Cache buster pour forcer le rechargement des graphiques à chaque visite de la page */
  private readonly cacheBuster = Date.now();

  get graphInscriptionsSrc(): string {
    return `/reports/graph_inscriptions_par_formation.png?t=${this.cacheBuster}`;
  }
  get graphCertificatsSrc(): string {
    return `/reports/graph_certificats_par_mois.png?t=${this.cacheBuster}`;
  }
}
