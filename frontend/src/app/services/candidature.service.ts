import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Candidature,
  CandidatureCreateRequest,
  Interview,
  InterviewScheduleRequest,
} from '../models/candidature.model';

@Injectable({ providedIn: 'root' })
export class CandidatureService {
  private readonly api = '/api/candidatures';
  private readonly interviewsApi = '/api/interviews';

  constructor(private http: HttpClient) {}

  listByProject(projectId: number, clientId: number): Observable<Candidature[]> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.get<Candidature[]>(`${this.api}/project/${projectId}`, { params });
  }

  create(request: CandidatureCreateRequest): Observable<Candidature> {
    return this.http.post<Candidature>(this.api, request);
  }

  listByFreelancer(freelancerId: number): Observable<Candidature[]> {
    return this.http.get<Candidature[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  getInterviews(candidatureId: number, clientId: number): Observable<Interview[]> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.get<Interview[]>(`${this.interviewsApi}/candidature/${candidatureId}`, { params });
  }

  scheduleInterview(
    candidatureId: number,
    clientId: number,
    body: InterviewScheduleRequest
  ): Observable<Interview> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.post<Interview>(`${this.interviewsApi}/candidature/${candidatureId}`, body, { params });
  }

  updateInterview(
    candidatureId: number,
    interviewId: number,
    clientId: number,
    body: InterviewScheduleRequest
  ): Observable<Interview> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.put<Interview>(
      `${this.interviewsApi}/candidature/${candidatureId}/${interviewId}`,
      body,
      { params }
    );
  }

  deleteInterview(candidatureId: number, interviewId: number, clientId: number): Observable<void> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.delete<void>(`${this.interviewsApi}/candidature/${candidatureId}/${interviewId}`, {
      params,
    });
  }

  accept(candidatureId: number, clientId: number): Observable<Candidature> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.put<Candidature>(`${this.api}/${candidatureId}/accept`, {}, { params });
  }

  reject(candidatureId: number, clientId: number): Observable<Candidature> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.put<Candidature>(`${this.api}/${candidatureId}/reject`, {}, { params });
  }

  payContract(contractId: number, clientId: number): Observable<void> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.put<void>(`${this.api}/contract/${contractId}/pay`, {}, { params });
  }

  cancelContract(contractId: number, clientId: number): Observable<void> {
    const params = new HttpParams().set('clientId', String(clientId));
    return this.http.put<void>(`${this.api}/contract/${contractId}/cancel`, {}, { params });
  }
}
