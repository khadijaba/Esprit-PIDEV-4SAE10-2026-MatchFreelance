import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Candidature, CandidatureRequest } from '../models/candidature.model';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private readonly api = '/api/candidatures';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(this.api);
  }

  getById(id: number): Observable<Candidature> {
    return this.http.get<Candidature>(`${this.api}/${id}`);
  }

  getByProjectId(projectId: number): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(`${this.api}/project/${projectId}`);
  }

  getByFreelancerId(freelancerId: number): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  create(req: CandidatureRequest): Observable<Candidature> {
    return this.http.post<Candidature>(this.api, req);
  }

  update(id: number, req: CandidatureRequest): Observable<Candidature> {
    return this.http.put<Candidature>(`${this.api}/${id}`, req);
  }

  accept(id: number, clientId: number = 1): Observable<Candidature> {
    return this.http.put<Candidature>(`${this.api}/${id}/accept?clientId=${clientId}`, {});
  }

  reject(id: number): Observable<Candidature> {
    return this.http.put<Candidature>(`${this.api}/${id}/reject`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
