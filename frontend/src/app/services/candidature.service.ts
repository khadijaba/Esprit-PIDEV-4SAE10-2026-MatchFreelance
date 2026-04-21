import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CandidatureRequest, CandidatureResponse } from '../models/candidature.model';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private readonly api = '/api/candidatures';

  constructor(private http: HttpClient) {}

  listByProject(projectId: number, clientId: number): Observable<CandidatureResponse[]> {
    return this.http.get<CandidatureResponse[]>(`${this.api}/project/${projectId}?clientId=${clientId}`);
  }

  getByFreelancerId(freelancerId: number): Observable<CandidatureResponse[]> {
    return this.http.get<CandidatureResponse[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  create(req: CandidatureRequest): Observable<CandidatureResponse> {
    return this.http.post<CandidatureResponse>(this.api, req);
  }

  update(id: number, req: CandidatureRequest): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.api}/${id}`, req);
  }

  accept(id: number, clientId: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.api}/${id}/accept?clientId=${clientId}`, {});
  }

  reject(id: number, clientId: number): Observable<CandidatureResponse> {
    return this.http.put<CandidatureResponse>(`${this.api}/${id}/reject?clientId=${clientId}`, {});
  }

  payContract(contractId: number, clientId: number): Observable<void> {
    return this.http.put<void>(`${this.api}/contract/${contractId}/pay?clientId=${clientId}`, {});
  }

  cancelContract(contractId: number, clientId: number): Observable<void> {
    return this.http.put<void>(`${this.api}/contract/${contractId}/cancel?clientId=${clientId}`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
