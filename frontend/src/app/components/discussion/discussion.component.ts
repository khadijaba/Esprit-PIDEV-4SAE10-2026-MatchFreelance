import { Component, OnInit, OnDestroy, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { ChatService } from '../../services/chat.service';
import { ToastService } from '../../services/toast.service';
import { DiscussionNavigationService } from '../../services/discussion-navigation.service';
import { ChatMessage } from '../../models/chat.model';

@Component({
  selector: 'app-discussion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './discussion.component.html',
})
export class DiscussionComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesEnd') messagesEnd!: ElementRef<HTMLDivElement>;

  currentUserId = 0;
  otherUserId = 0;
  otherUserName = '';
  messages = signal<ChatMessage[]>([]);
  newText = '';
  loading = true;
  private scrollToBottom = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
    private userService: UserService,
    private chatService: ChatService,
    private toast: ToastService,
    private discussionNav: DiscussionNavigationService,
    private location: Location
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    if (!user?.id) {
      this.toast.error('Connectez-vous pour discuter');
      this.router.navigate(['/login']);
      return;
    }
    this.currentUserId = Number(user.id);

    const idParam = this.route.snapshot.paramMap.get('userId');
    const otherId = idParam ? parseInt(idParam, 10) : 0;
    if (!otherId || isNaN(otherId)) {
      this.toast.error('Discussion introuvable');
      this.router.navigate(['/']);
      return;
    }
    if (otherId === this.currentUserId) {
      this.toast.error('Vous ne pouvez pas ouvrir une discussion avec vous-même');
      this.router.navigate(['/']);
      return;
    }
    this.otherUserId = otherId;
    const pendingName = this.discussionNav.getAndClearPendingName();
    if (pendingName?.trim()) this.otherUserName = pendingName.trim();

    this.userService.getById(this.otherUserId).subscribe({
      next: (u) => {
        const name = u?.fullName ?? u?.username ?? u?.email;
        if (name) this.otherUserName = name;
        this.loading = false;
        this.refreshMessages();
      },
      error: () => {
        if (!this.otherUserName) this.otherUserName = `Utilisateur #${this.otherUserId}`;
        this.loading = false;
        this.refreshMessages();
      },
    });
  }

  ngOnDestroy(): void {}

  private refreshMessages(): void {
    this.messages.set(this.chatService.getMessages(this.currentUserId, this.otherUserId));
    this.scrollToBottom = true;
  }

  ngAfterViewChecked(): void {
    if (this.scrollToBottom && this.messagesEnd?.nativeElement) {
      this.messagesEnd.nativeElement.scrollIntoView({ behavior: 'smooth' });
      this.scrollToBottom = false;
    }
  }

  close(): void {
    this.location.back();
  }

  setSuggestion(text: string): void {
    this.newText = text;
  }

  setSuggestionGreeting(): void {
    this.newText = `👋 Bonjour ${this.otherUserName}, pouvez-vous m'aider avec...`;
  }

  send(): void {
    const text = this.newText.trim().slice(0, 2500);
    if (!text) return;
    this.chatService.sendMessage(this.currentUserId, this.otherUserId, text);
    this.newText = '';
    this.refreshMessages();
  }

  isFromMe(msg: ChatMessage): boolean {
    return msg.fromUserId === this.currentUserId;
  }

  formatTime(iso: string): string {
    const d = new Date(iso);
    const now = new Date();
    const sameDay = d.toDateString() === now.toDateString();
    if (sameDay) return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }
}
