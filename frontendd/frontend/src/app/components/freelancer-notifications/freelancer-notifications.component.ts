import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../models/notification.model';

@Component({
  selector: 'app-freelancer-notifications',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './freelancer-notifications.component.html',
})
export class FreelancerNotificationsComponent implements OnInit {
  notifications: Notification[] = [];
  selectedId: string | null = null;
  loading = true;

  constructor(
    private auth: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.auth.getCurrentUser()?.id;
    const freelancerId = id != null ? Number(id) : 0;
    this.notificationService.getByFreelancerId(freelancerId).subscribe((list) => {
      this.notifications = list;
      this.loading = false;
    });
  }

  get selected(): Notification | undefined {
    return this.selectedId ? this.notificationService.getById(this.selectedId) : undefined;
  }

  openNotification(n: Notification): void {
    this.selectedId = n.id;
    this.notificationService.markAsRead(n.id);
  }

  closeMessage(): void {
    this.selectedId = null;
  }

  formatDate(iso: string): string {
    const d = new Date(iso);
    const now = new Date();
    const sameDay = d.toDateString() === now.toDateString();
    if (sameDay) return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }
}
