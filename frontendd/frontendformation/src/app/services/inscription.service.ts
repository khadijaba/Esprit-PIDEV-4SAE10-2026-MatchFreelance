import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Inscription } from '../models/inscription.model';

@Injectable({ providedIn: 'root' })
export class InscriptionService {
  private readonly api = '/api/inscriptions';

  constructor(private http: HttpClient) {}

  getByFormation(formationId: number): Observable<Inscription[]> {
    return this.http.get<Inscription[]>(`${this.api}/formation/${formationId}`);
  }

  getByFreelancer(freelancerId: number): Observable<Inscription[]> {
    return this.http.get<Inscription[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  getEnAttente(): Observable<Inscription[]> {
    return this.http.get<Inscription[]>(`${this.api}/en-attente`);
  }

  inscrire(formationId: number, freelancerId: number): Observable<Inscription> {
    return this.http.post<Inscription>(
      `${this.api}/formation/${formationId}/freelancer/${freelancerId}`,
      {}
    );
  }

  valider(id: number): Observable<Inscription> {
    return this.http.patch<Inscription>(`${this.api}/${id}/valider`, {});
  }

  annuler(id: number): Observable<void> {
    return this.http.patch<void>(`${this.api}/${id}/annuler`, {});
  }
}
