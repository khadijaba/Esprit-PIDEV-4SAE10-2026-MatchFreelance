import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { ChatService } from '../../services/chat.service';
import { ToastService } from '../../services/toast.service';
import { DiscussionNavigationService } from '../../services/discussion-navigation.service';

interface ConversationItem {
  otherUserId: number;
  otherUserName: string;
  lastMessage: string;
  lastAt: string;
}

@Component({
  selector: 'app-discussions-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './discussions-list.component.html',
})
export class DiscussionsListComponent implements OnInit {
  conversations: ConversationItem[] = [];
  loading = true;

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private chatService: ChatService,
    private toast: ToastService,
    private discussionNav: DiscussionNavigationService,
    private router: Router
  ) {}

  openDiscussion(otherUserId: number, otherUserName: string): void {
    if (otherUserName?.trim()) this.discussionNav.setPendingName(otherUserName.trim());
    this.router.navigate(['/discussion', otherUserId]);
  }

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    if (!user?.id) {
      this.toast.error('Connectez-vous pour voir vos discussions');
      this.router.navigate(['/login']);
      return;
    }
    const currentUserId = Number(user.id);
    const partnerIds = this.chatService.getConversationPartnerIds(currentUserId);
    if (partnerIds.length === 0) {
      this.loading = false;
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
            lastMessage: last ? (last.text.slice(0, 60) + (last.text.length > 60 ? '…' : '')) : '',
            lastAt: last?.createdAt ?? '',
          });
          done++;
          if (done === partnerIds.length) {
            this.conversations.sort((a, b) => new Date(b.lastAt).getTime() - new Date(a.lastAt).getTime());
            this.loading = false;
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
            this.loading = false;
          }
        },
      });
    });
  }

  formatDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }
}
