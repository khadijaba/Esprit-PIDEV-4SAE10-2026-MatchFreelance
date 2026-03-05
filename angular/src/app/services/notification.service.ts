import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification, NotificationPage } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly baseUrl = '/api/notifications';

  constructor(private http: HttpClient) {}

  listForUser(userId: number, params?: { page?: number; size?: number; sort?: string }): Observable<NotificationPage> {
    let httpParams = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v === undefined || v === null || v === '') return;
        httpParams = httpParams.set(k, String(v));
      });
    }
    return this.http.get<NotificationPage>(`${this.baseUrl}/users/${userId}`, { params: httpParams });
  }

  markAsRead(notificationId: number, userId: number): Observable<Notification> {
    return this.http.post<Notification>(`${this.baseUrl}/${notificationId}/read`, null, {
      params: new HttpParams().set('userId', String(userId)),
    });
  }

  markAllAsRead(userId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/users/${userId}/read-all`, {});
  }
}

