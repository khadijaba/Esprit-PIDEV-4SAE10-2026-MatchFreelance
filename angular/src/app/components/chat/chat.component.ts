import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { ToastService } from '../../services/toast.service';
import { Message } from '../../models/message.model';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css',
})
export class ChatComponent implements OnInit {
  @Input({ required: true }) contractId!: number;
  @Input({ required: true }) currentUserId!: number;
  @Input() otherPartyLabel = 'Other';

  messages: Message[] = [];
  newMessage = '';
  loading = true;
  sending = false;

  constructor(
    private chatService: ChatService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.loadMessages();
  }

  loadMessages() {
    this.loading = true;
    this.chatService.getMessages(this.contractId).subscribe({
      next: (data) => {
        this.messages = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load messages');
      },
    });
  }

  send() {
    const content = this.newMessage.trim();
    if (!content || this.sending) return;

    this.sending = true;
    this.chatService
      .sendMessage(this.contractId, {
        senderId: this.currentUserId,
        content,
      })
      .subscribe({
        next: (msg) => {
          this.messages = [...this.messages, msg];
          this.newMessage = '';
          this.sending = false;
        },
        error: (err) => {
          this.sending = false;
          this.toast.error(err?.error?.message || 'Failed to send message');
        },
      });
  }

  isFromMe(msg: Message): boolean {
    return msg.senderId === this.currentUserId;
  }
}
