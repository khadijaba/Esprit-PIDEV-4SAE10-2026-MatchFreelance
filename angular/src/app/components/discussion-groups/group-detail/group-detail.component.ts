import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DiscussionGroupService } from '../../../services/discussion-group.service';
import { GroupMessageService, MessageType } from '../../../services/group-message.service';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-group-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './group-detail.component.html',
  styleUrls: ['./group-detail.component.css']
})
export class GroupDetailComponent implements OnInit, OnDestroy {
  groupId!: number;
  group: any = null;
  messages: any[] = [];
  messageContent: string = '';
  currentUserId: number = 1; // Mock user ID
  currentUserName: string = 'Test User'; // Mock user name
  loading: boolean = false;
  sending: boolean = false;
  showEmojiPicker: boolean = false;
  showGifPicker: boolean = false;
  private refreshSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private groupService: DiscussionGroupService,
    private messageService: GroupMessageService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.groupId = +params['id'];
      this.loadGroup();
      this.loadMessages();
      this.startAutoRefresh();
    });
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  loadGroup(): void {
    this.loading = true;
    this.groupService.getGroupById(this.groupId).subscribe({
      next: (group) => {
        this.group = group;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading group:', error);
        this.loading = false;
      }
    });
  }

  loadMessages(): void {
    this.messageService.getGroupMessages(this.groupId).subscribe({
      next: (messages) => {
        this.messages = messages;
        setTimeout(() => this.scrollToBottom(), 100);
      },
      error: (error) => {
        console.error('Error loading messages:', error);
      }
    });
  }

  startAutoRefresh(): void {
    // Refresh messages every 5 seconds
    this.refreshSubscription = interval(5000).subscribe(() => {
      this.loadMessages();
    });
  }

  sendMessage(): void {
    if (!this.messageContent.trim() || this.sending) {
      return;
    }

    this.sending = true;
    const message = {
      senderId: this.currentUserId,
      senderName: this.currentUserName,
      content: this.messageContent,
      type: MessageType.TEXT
    };

    this.messageService.sendMessage(this.groupId, message).subscribe({
      next: (savedMessage) => {
        this.messages.push(savedMessage);
        this.messageContent = '';
        this.sending = false;
        setTimeout(() => this.scrollToBottom(), 100);
      },
      error: (error) => {
        console.error('Error sending message:', error);
        this.sending = false;
      }
    });
  }

  isMyMessage(message: any): boolean {
    return message.senderId === this.currentUserId;
  }

  getMessageTime(sentAt: string): string {
    const date = new Date(sentAt);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays}d ago`;
    
    return date.toLocaleDateString();
  }

  getUserInitials(name: string): string {
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  scrollToBottom(): void {
    const messagesContainer = document.querySelector('.messages-container');
    if (messagesContainer) {
      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
  }

  toggleEmojiPicker(): void {
    this.showEmojiPicker = !this.showEmojiPicker;
    this.showGifPicker = false;
  }

  toggleGifPicker(): void {
    this.showGifPicker = !this.showGifPicker;
    this.showEmojiPicker = false;
  }

  addEmoji(emoji: string): void {
    this.messageContent += emoji;
    this.showEmojiPicker = false;
  }

  goToSettings(): void {
    this.router.navigate(['/groups', this.groupId, 'settings']);
  }

  goToMembers(): void {
    this.router.navigate(['/groups', this.groupId, 'members']);
  }

  goBack(): void {
    this.router.navigate(['/groups']);
  }
}
