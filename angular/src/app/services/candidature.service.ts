import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { Candidature, CandidatureRequest, CandidatureStatus } from '../models/candidature.model';
import { PageResponse } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private readonly api = '/api/candidatures';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(this.api);
  }

  getPage(params: {
    projectId?: number;
    freelancerId?: number;
    status?: CandidatureStatus;
    page?: number;
    size?: number;
    sort?: string;
  }): Observable<PageResponse<Candidature>> {
    let httpParams = new HttpParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v === undefined || v === null || v === '') return;
      httpParams = httpParams.set(k, String(v));
    });
    return this.http.get<PageResponse<Candidature>>(`${this.api}/page`, { params: httpParams });
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
