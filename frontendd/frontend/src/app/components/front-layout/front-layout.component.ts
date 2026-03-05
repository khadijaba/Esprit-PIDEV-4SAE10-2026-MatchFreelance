import { Component, HostListener, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { UserService } from '../../services/user.service';
import { ChatService } from '../../services/chat.service';
import { DiscussionNavigationService } from '../../services/discussion-navigation.service';
import { Notification } from '../../models/notification.model';

interface ConversationItem {
  otherUserId: number;
  otherUserName: string;
  lastMessage: string;
  lastAt: string;
}

@Component({
  selector: 'app-front-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './front-layout.component.html',
})
export class FrontLayoutComponent {
  @ViewChild('notifWrap') notifWrapRef?: ElementRef<HTMLElement>;
  @ViewChild('discWrap') discWrapRef?: ElementRef<HTMLElement>;

  /** Badge non lus : réagit aux nouveaux envois (items$) et au freelancer connecté. */
  unreadCount$: Observable<number>;

  showNotificationsPanel = false;
  showDiscussionsPanel = false;
  notifications: Notification[] = [];
  loadingNotifications = false;
  conversations: ConversationItem[] = [];
  loadingDiscussions = false;

  constructor(
    public auth: AuthService,
    private notificationService: NotificationService,
    private userService: UserService,
    private chatService: ChatService,
    private discussionNav: DiscussionNavigationService,
    private router: Router
  ) {
    this.unreadCount$ = this.notificationService.items$.pipe(
      map(() => this.notificationService.unreadCount(this.freelancerId))
    );
  }

  get freelancerId(): number {
    const id = this.auth.getCurrentUser()?.id;
    return id != null ? Number(id) : 0;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(e: Event): void {
    const t = e.target as HTMLElement;
    if (this.notifWrapRef?.nativeElement?.contains(t) || this.discWrapRef?.nativeElement?.contains(t)) return;
    this.showNotificationsPanel = false;
    this.showDiscussionsPanel = false;
  }

  toggleNotifications(): void {
    this.showDiscussionsPanel = false;
    this.showNotificationsPanel = !this.showNotificationsPanel;
    if (this.showNotificationsPanel) this.loadNotifications();
  }

  toggleDiscussions(): void {
    this.showNotificationsPanel = false;
    this.showDiscussionsPanel = !this.showDiscussionsPanel;
    if (this.showDiscussionsPanel) this.loadConversations();
  }

  loadNotifications(): void {
    this.loadingNotifications = true;
    this.notificationService.getByFreelancerId(this.freelancerId).subscribe((list) => {
      this.notifications = list;
      this.loadingNotifications = false;
    });
  }

  loadConversations(): void {
    const user = this.auth.getCurrentUser();
    if (!user?.id) {
      this.loadingDiscussions = false;
      return;
    }
    this.loadingDiscussions = true;
    this.conversations = [];
    const currentUserId = Number(user.id);
    const partnerIds = this.chatService.getConversationPartnerIds(currentUserId);
    if (partnerIds.length === 0) {
      this.loadingDiscussions = false;
      return;
    }
    let done = 0;
    partnerIds.forEach((otherId) => {
      this.userService.getById(otherId).subscribe({
        next: (u) => {
          const name = u?.fullName ?? u?.username ?? u?.email ?? `Utilisateur #${otherId}`;
          const messages = this.chatService.getMessages(currentUserId, otherId);
          const last = messages[messages.length - 1];
          this.conversations.push({
            otherUserId: otherId,
            otherUserName: name,
            lastMessage: last ? (last.text.slice(0, 50) + (last.text.length > 50 ? '…' : '')) : '',
            lastAt: last?.createdAt ?? '',
          });
          done++;
          if (done === partnerIds.length) {
            this.conversations.sort((a, b) => new Date(b.lastAt).getTime() - new Date(a.lastAt).getTime());
            this.loadingDiscussions = false;
          }
        },
        error: () => {
          this.conversations.push({
            otherUserId: otherId,
            otherUserName: `Utilisateur #${otherId}`,
            lastMessage: '',
            lastAt: '',
          });
          done++;
          if (done === partnerIds.length) {
            this.conversations.sort((a, b) => new Date(b.lastAt).getTime() - new Date(a.lastAt).getTime());
            this.loadingDiscussions = false;
          }
        },
      });
    });
  }

  openNotification(n: Notification): void {
    this.notificationService.markAsRead(n.id);
  }

  openDiscussion(otherUserId: number, otherUserName: string): void {
    this.showDiscussionsPanel = false;
    if (otherUserName?.trim()) this.discussionNav.setPendingName(otherUserName.trim());
    this.router.navigate(['/discussion', otherUserId]);
  }

  formatNotifDate(iso: string): string {
    const d = new Date(iso);
    const now = new Date();
    const sameDay = d.toDateString() === now.toDateString();
    if (sameDay) return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
  }

  formatDiscDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }
}
