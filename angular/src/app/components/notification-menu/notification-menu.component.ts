import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Notification } from '../../models/notification.model';
import { NotificationService } from '../../services/notification.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-notification-menu',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-menu.component.html',
})
export class NotificationMenuComponent {
  @Input() userId!: number;

  open = false;
  loading = false;
  notifications: Notification[] = [];
  private loadedOnce = false;

  constructor(
    private notificationService: NotificationService,
    private toast: ToastService
  ) {}

  get unreadCount(): number {
    return this.notifications.filter((n) => !n.readAt).length;
  }

  toggle() {
    this.open = !this.open;
    if (this.open && !this.loadedOnce) {
      this.load();
    }
  }

  load() {
    if (!this.userId) return;
    this.loading = true;
    this.notificationService
      .listForUser(this.userId, { page: 0, size: 10, sort: 'createdAt,desc' })
      .subscribe({
        next: (page) => {
          this.notifications = page.content;
          this.loadedOnce = true;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.toast.error('Failed to load notifications');
        },
      });
  }

  markAsRead(n: Notification) {
    if (!this.userId || n.readAt) return;
    this.notificationService.markAsRead(n.id, this.userId).subscribe({
      next: (updated) => {
        this.notifications = this.notifications.map((x) => (x.id === updated.id ? updated : x));
      },
      error: () => this.toast.error('Failed to mark as read'),
    });
  }

  markAllAsRead() {
    if (!this.userId || this.unreadCount === 0) return;
    this.notificationService.markAllAsRead(this.userId).subscribe({
      next: () => {
        this.notifications = this.notifications.map((n) => ({ ...n, readAt: n.readAt ?? new Date().toISOString() }));
      },
      error: () => this.toast.error('Failed to mark all as read'),
    });
  }
}

