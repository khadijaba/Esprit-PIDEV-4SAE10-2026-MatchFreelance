import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import { Notification } from '../models/notification.model';
import type { NotificationToSend } from '../models/team-ai.model';
import { NotificationApiService } from './notification-api.service';

const STORAGE_KEY = 'app_notifications';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  /** Exposé pour que le header puisse réagir aux nouveaux envois (badge). */
  readonly items$ = new BehaviorSubject<Notification[]>(this.loadFromStorage());

  constructor(private notificationApi: NotificationApiService) {}

  private loadFromStorage(): Notification[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const arr = JSON.parse(raw) as Notification[];
        return Array.isArray(arr) ? arr : [];
      }
    } catch {
      // ignore
    }
    return [];
  }

  /** Recharge la liste depuis le localStorage (utile après ouverture dans un autre onglet ou après envoi côté client). */
  refreshFromStorage(): void {
    const list = this.loadFromStorage();
    this.items$.next(list);
  }

  private saveToStorage(list: Notification[]): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    } catch {
      // ignore
    }
    this.items$.next(list);
  }

  /**
   * Envoie les notifications aux freelancers : enregistrement local immédiat + persistance backend (API).
   * Les freelancers pourront voir les notifications sur tout appareil via l'API.
   */
  /**
   * Notifie le(s) freelancer(s) assigné(s) qu'un meeting de phase a été programmé (supervision).
   */
  notifyPhaseMeetingScheduled(
    projectId: number,
    projectTitle: string,
    phaseName: string,
    meetingAtIso: string,
    freelancers: { freelancerId: number; email?: string }[]
  ): void {
    if (!freelancers.length) return;
    const when = new Date(meetingAtIso).toLocaleString('fr-FR', {
      dateStyle: 'full',
      timeStyle: 'short',
    });
    const notifications: NotificationToSend[] = freelancers.map((f) => ({
      freelancerId: f.freelancerId,
      email: f.email,
      subject: `Meeting programmé — ${projectTitle}`,
      message:
        `Un meeting de supervision a été planifié pour le projet « ${projectTitle} », phase « ${phaseName} », le ${when}. ` +
        `Consultez-le dans Espace freelancer → Suivi de mes missions : vous pouvez rejoindre la salle visio depuis la liste des meetings.`,
    }));
    this.sendBulk(projectId, projectTitle, notifications);
  }

  sendBulk(projectId: number, projectTitle: string, notifications: NotificationToSend[]): void {
    const list = this.items$.value.slice();
    const now = new Date().toISOString();
    for (const n of notifications) {
      const fid = Number(n.freelancerId);
      const id = `n-${Date.now()}-${fid}-${Math.random().toString(36).slice(2, 9)}`;
      list.push({
        id,
        freelancerId: fid,
        projectId,
        projectTitle,
        subject: n.subject ?? `Proposition d'équipe – ${projectTitle}`,
        message: n.message,
        read: false,
        createdAt: now,
        email: n.email,
      });
    }
    this.saveToStorage(list);

    this.notificationApi.saveBulk(projectId, projectTitle, notifications).subscribe({
      error: () => {},
    });
  }

  private normalizeNotification(n: Partial<Notification> & { id?: string | number }): Notification {
    return {
      id: n.id != null ? String(n.id) : `n-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
      freelancerId: Number(n.freelancerId),
      projectId: Number(n.projectId),
      projectTitle: n.projectTitle ?? '',
      subject: n.subject ?? '',
      message: n.message ?? '',
      read: Boolean(n.read),
      createdAt: n.createdAt ?? new Date().toISOString(),
      email: n.email,
    };
  }

  getByFreelancerId(freelancerId: number): Observable<Notification[]> {
    const fid = Number(freelancerId);
    const sortByDate = (list: Notification[]) =>
      [...list].sort((a, b) => (b.createdAt > a.createdAt ? 1 : -1));

    return this.notificationApi.getByFreelancerId(fid).pipe(
      map((list) => list.map((n) => this.normalizeNotification(n))),
      tap((list) => {
        this.saveToStorage(list);
        this.items$.next(list);
      }),
      map(sortByDate),
      catchError(() => {
        this.refreshFromStorage();
        const list = this.items$
          .getValue()
          .filter((n) => Number(n.freelancerId) === fid);
        return of(sortByDate(list));
      })
    );
  }

  getById(id: string): Notification | undefined {
    return this.items$.value.find((n) => n.id === id);
  }

  getById$(id: string): Observable<Notification | undefined> {
    return this.items$.pipe(map((list) => list.find((n) => n.id === id)));
  }

  markAsRead(id: string): void {
    const list = this.items$.value.map((n) => (n.id === id ? { ...n, read: true } : n));
    this.saveToStorage(list);
    this.notificationApi.markAsRead(id).subscribe({ error: () => {} });
  }

  unreadCount(freelancerId: number): number {
    const fid = Number(freelancerId);
    return this.items$.value.filter((n) => Number(n.freelancerId) === fid && !n.read).length;
  }

  unreadCount$(freelancerId: number): Observable<number> {
    const fid = Number(freelancerId);
    return this.items$.pipe(
      map((list) => list.filter((n) => Number(n.freelancerId) === fid && !n.read).length)
    );
  }
}
