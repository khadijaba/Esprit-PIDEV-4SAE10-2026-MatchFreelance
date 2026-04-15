import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import type {
  ComputeCompatibleRequest,
  ComputeCompatibleResponse,
} from '../models/team-ai.model';

/**
 * Appelle le service Python (team-ai) pour calculer les freelancers compatibles avec un projet.
 * Utilisé après publication d'un projet pour envoyer des notifications automatiques (score >= 70%).
 */
@Injectable({ providedIn: 'root' })
export class CompatibilityNotifierService {
  /** Proxy Angular : /api/ai -> Python (port 5000) */
  private readonly api = '/api/ai/compute-compatible-freelancers';

  constructor(
    private http: HttpClient,
    private auth: AuthService
  ) {}

  /**
   * Calcule les freelancers dont le score de compatibilité avec le projet est >= 70%.
   * Le backend Python récupère skills et users, calcule le score (compétences, expérience, note, projets).
   */
  computeCompatibleFreelancers(request: ComputeCompatibleRequest): Observable<ComputeCompatibleResponse> {
    const token = this.auth.getToken();
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.post<ComputeCompatibleResponse>(this.api, request, { headers });
  }
}
