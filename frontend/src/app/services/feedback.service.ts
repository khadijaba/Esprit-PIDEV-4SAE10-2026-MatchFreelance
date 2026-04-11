import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Feedback, CreateFeedbackRequest, FeedbackStats, FeedbackStatus, FeedbackCategory } from '../models/feedback.model';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private api = '/api/feedbacks';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(this.api);
  }

  getById(id: number): Observable<Feedback> {
    return this.http.get<Feedback>(`${this.api}/${id}`);
  }

  getByEventId(eventId: number): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.api}/event/${eventId}`);
  }

  getApprovedByEventId(eventId: number): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.api}/event/${eventId}/approved`);
  }

  getStatsByEventId(eventId: number): Observable<FeedbackStats> {
    return this.http.get<FeedbackStats>(`${this.api}/event/${eventId}/stats`);
  }

  getByStatus(status: FeedbackStatus): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.api}/status/${status}`);
  }

  getByCategory(category: FeedbackCategory): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.api}/category/${category}`);
  }

  create(request: CreateFeedbackRequest): Observable<Feedback> {
    return this.http.post<Feedback>(this.api, request);
  }

  update(id: number, request: CreateFeedbackRequest): Observable<Feedback> {
    return this.http.put<Feedback>(`${this.api}/${id}`, request);
  }

  approve(id: number): Observable<Feedback> {
    return this.http.patch<Feedback>(`${this.api}/${id}/approve`, {});
  }

  reject(id: number): Observable<Feedback> {
    return this.http.patch<Feedback>(`${this.api}/${id}/reject`, {});
  }

  flag(id: number): Observable<Feedback> {
    return this.http.patch<Feedback>(`${this.api}/${id}/flag`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
