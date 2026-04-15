import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Interview, InterviewRequest } from '../models/interview.model';

@Injectable({ providedIn: 'root' })
export class InterviewService {
  private readonly api = '/api/candidatures';

  constructor(private http: HttpClient) {}

  getByCandidatureId(candidatureId: number): Observable<Interview[]> {
    return this.http.get<Interview[]>(`${this.api}/${candidatureId}/interviews`);
  }

  schedule(candidatureId: number, interview: InterviewRequest): Observable<Interview> {
    return this.http.post<Interview>(`${this.api}/${candidatureId}/interviews`, interview);
  }

  update(candidatureId: number, interviewId: number, interview: InterviewRequest): Observable<Interview> {
    return this.http.put<Interview>(`${this.api}/${candidatureId}/interviews/${interviewId}`, interview);
  }

  delete(candidatureId: number, interviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${candidatureId}/interviews/${interviewId}`);
  }
}
