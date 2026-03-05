import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Candidature, CandidatureRequest } from '../models/candidature.model';
import { RankedCandidature, BudgetStats } from '../models/ranking.model';

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

  getRankedByProjectId(projectId: number, minScore?: number, limit?: number): Observable<RankedCandidature[]> {
    let params = new HttpParams();
    if (minScore != null) params = params.set('minScore', minScore);
    if (limit != null) params = params.set('limit', limit);
    return this.http.get<RankedCandidature[]>(`${this.api}/project/${projectId}/ranked`, { params });
  }

  getBudgetStatsByProjectId(projectId: number): Observable<BudgetStats> {
    return this.http.get<BudgetStats>(`${this.api}/project/${projectId}/budget-stats`);
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

  payContract(contractId: number, clientId: number = 1): Observable<void> {
    return this.http.put<void>(`${this.api}/contract/${contractId}/pay?clientId=${clientId}`, {});
  }

  cancelContract(contractId: number, clientId: number = 1): Observable<void> {
    return this.http.put<void>(`${this.api}/contract/${contractId}/cancel?clientId=${clientId}`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
