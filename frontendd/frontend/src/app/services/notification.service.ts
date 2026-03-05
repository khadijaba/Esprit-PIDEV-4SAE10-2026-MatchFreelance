import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Notification } from '../models/notification.model';
import type { NotificationToSend } from '../models/team-ai.model';

const STORAGE_KEY = 'app_notifications';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  /** Exposé pour que le header puisse réagir aux nouveaux envois (badge). */
  readonly items$ = new BehaviorSubject<Notification[]>(this.loadFromStorage());

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

  private saveToStorage(list: Notification[]): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    } catch {
      // ignore
    }
    this.items$.next(list);
  }

  /**
   * Envoie les notifications aux freelancers (stockage local ; à remplacer par appel API + email backend).
   */
  sendBulk(projectId: number, projectTitle: string, notifications: NotificationToSend[]): void {
    const list = this.items$.value.slice();
    const now = new Date().toISOString();
    for (const n of notifications) {
      const id = `n-${Date.now()}-${n.freelancerId}-${Math.random().toString(36).slice(2, 9)}`;
      list.push({
        id,
        freelancerId: n.freelancerId,
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
  }

  getByFreelancerId(freelancerId: number): Observable<Notification[]> {
    return this.items$.pipe(
      map((list) => list.filter((n) => n.freelancerId === freelancerId).sort((a, b) => (b.createdAt > a.createdAt ? 1 : -1)))
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
  }

  unreadCount(freelancerId: number): number {
    return this.items$.value.filter((n) => n.freelancerId === freelancerId && !n.read).length;
  }

  unreadCount$(freelancerId: number): Observable<number> {
    return this.items$.pipe(
      map((list) => list.filter((n) => n.freelancerId === freelancerId && !n.read).length)
    );
  }
}
