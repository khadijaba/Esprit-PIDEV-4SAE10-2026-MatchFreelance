import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Interview, InterviewRequest } from '../models/interview.model';
import { InterviewMetrics } from '../models/interview-metrics.model';

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private readonly api = '/api/interviews';

  constructor(private http: HttpClient) {}

  getMetrics(candidatureId: number, requestingUserId: number): Observable<InterviewMetrics> {
    return this.http.get<InterviewMetrics>(
      `${this.api}/candidature/${candidatureId}/metrics?requestingUserId=${requestingUserId}`
    );
  }

  /** Liste en lecture seule pour le freelancer (planning / statuts). */
  getForFreelancer(candidatureId: number, freelancerId: number): Observable<Interview[]> {
    return this.http.get<Interview[]>(
      `${this.api}/candidature/${candidatureId}/freelancer?freelancerId=${freelancerId}`
    );
  }

  getByCandidatureId(candidatureId: number, clientId: number): Observable<Interview[]> {
    return this.http.get<Interview[]>(`${this.api}/candidature/${candidatureId}?clientId=${clientId}`);
  }

  schedule(candidatureId: number, clientId: number, interview: InterviewRequest): Observable<Interview> {
    return this.http.post<Interview>(`${this.api}/candidature/${candidatureId}?clientId=${clientId}`, interview);
  }

  update(candidatureId: number, interviewId: number, clientId: number, interview: InterviewRequest): Observable<Interview> {
    return this.http.put<Interview>(
      `${this.api}/candidature/${candidatureId}/${interviewId}?clientId=${clientId}`,
      interview
    );
  }

  delete(candidatureId: number, interviewId: number, clientId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/candidature/${candidatureId}/${interviewId}?clientId=${clientId}`);
  }
}
