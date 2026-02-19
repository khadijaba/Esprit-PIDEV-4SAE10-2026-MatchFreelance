import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Certificat,
  Examen,
  ExamenRequest,
  PassageExamen,
  ReponseExamenRequest,
} from '../models/examen.model';

/**
 * URL relative : le proxy (ng serve) envoie /api/* vers http://localhost:8082.
 * Ainsi pas de CORS (même origine localhost:4200).
 */
@Injectable({ providedIn: 'root' })
export class ExamenService {
  private readonly api = '/api/examens';
  private readonly apiCertificats = '/api/certificats';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Examen[]> {
    return this.http.get<Examen[]>(this.api);
  }

  getByFormation(formationId: number): Observable<Examen[]> {
    return this.http.get<Examen[]>(`${this.api}/formation/${formationId}`);
  }

  getById(id: number): Observable<Examen> {
    return this.http.get<Examen>(`${this.api}/${id}`);
  }

  getPourPassage(id: number): Observable<Examen> {
    return this.http.get<Examen>(`${this.api}/${id}/passage`);
  }

  create(dto: ExamenRequest): Observable<Examen> {
    return this.http.post<Examen>(this.api, dto);
  }

  update(id: number, dto: Partial<ExamenRequest>): Observable<Examen> {
    return this.http.put<Examen>(`${this.api}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  passerExamen(examenId: number, request: ReponseExamenRequest): Observable<PassageExamen> {
    return this.http.post<PassageExamen>(`${this.api}/${examenId}/passer`, request);
  }

  getResultatsByFreelancer(freelancerId: number): Observable<PassageExamen[]> {
    return this.http.get<PassageExamen[]>(`${this.api}/resultats/freelancer/${freelancerId}`);
  }

  getResultatsByExamen(examenId: number): Observable<PassageExamen[]> {
    return this.http.get<PassageExamen[]>(`${this.api}/${examenId}/resultats`);
  }

  getPassage(examenId: number, freelancerId: number): Observable<PassageExamen> {
    return this.http.get<PassageExamen>(`${this.api}/${examenId}/freelancer/${freelancerId}`);
  }

  getCertificatByPassage(passageExamenId: number): Observable<Certificat> {
    return this.http.get<Certificat>(`${this.apiCertificats}/passage/${passageExamenId}`);
  }

  getCertificatById(id: number): Observable<Certificat> {
    return this.http.get<Certificat>(`${this.apiCertificats}/${id}`);
  }

  getCertificatsByFreelancer(freelancerId: number): Observable<Certificat[]> {
    return this.http.get<Certificat[]>(`${this.apiCertificats}/freelancer/${freelancerId}`);
  }

  /** Télécharge le PDF du certificat via le proxy puis ouvre-le dans un nouvel onglet (évite 404 en navigation directe). */
  getCertificatPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiCertificats}/${id}/pdf`, { responseType: 'blob' });
  }
}
