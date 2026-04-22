import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification } from '../models/notification.model';
import type { NotificationToSend } from '../models/team-ai.model';

/** Corps de la requête pour l'envoi en masse de notifications (backend). */
export interface NotificationsBulkRequest {
  projectId: number;
  projectTitle: string;
  notifications: NotificationToSend[];
}

/** Réponse du backend après création en masse (optionnel : liste des notifications créées). */
export interface NotificationsBulkResponse {
  notifications?: Notification[];
  created?: number;
}

/**
 * Service d'accès à l'API backend des notifications.
 * Permet de persister les notifications en base pour que les freelancers les voient sur tout appareil.
 */
@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly api = '/api/notifications';

  constructor(private http: HttpClient) {}

  /**
   * Enregistre les notifications en base (appelé après création d'un projet pour les freelancers compatibles).
   */
  saveBulk(
    projectId: number,
    projectTitle: string,
    notifications: NotificationToSend[]
  ): Observable<NotificationsBulkResponse> {
    const body: NotificationsBulkRequest = { projectId, projectTitle, notifications };
    return this.http.post<NotificationsBulkResponse>(`${this.api}/bulk`, body);
  }

  /**
   * Récupère toutes les notifications d'un freelancer (pour affichage liste + badge).
   */
  getByFreelancerId(freelancerId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.api}/freelancer/${freelancerId}`);
  }

  /**
   * Marque une notification comme lue (persistance backend).
   */
  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${this.api}/${encodeURIComponent(id)}/read`, {});
  }
}
