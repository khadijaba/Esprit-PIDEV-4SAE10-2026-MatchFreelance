import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export enum PrivateMessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  GIF = 'GIF',
  FILE = 'FILE',
  EMOJI = 'EMOJI'
}

export interface PrivateMessage {
  id?: number;
  senderId: number;
  receiverId: number;
  senderName: string;
  receiverName: string;
  senderAvatar?: string;
  content: string;
  type: PrivateMessageType;
  mediaUrl?: string;
  gifUrl?: string;
  sentAt?: Date;
  readAt?: Date;
  isRead?: boolean;
  isDeleted?: boolean;
  isDeletedBySender?: boolean;
  isDeletedByReceiver?: boolean;
  replyToMessageId?: number;
  reaction?: string;
}

export interface PrivateMessageRequest {
  senderId: number;
  receiverId: number;
  senderName: string;
  receiverName: string;
  senderAvatar?: string;
  content: string;
  type?: PrivateMessageType;
  mediaUrl?: string;
  gifUrl?: string;
  replyToMessageId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PrivateMessageService {
  private apiUrl = `${environment.apiUrl}/messages/private`;

  constructor(private http: HttpClient) {}

  /**
   * Send a private message
   */
  sendMessage(request: PrivateMessageRequest): Observable<PrivateMessage> {
    return this.http.post<PrivateMessage>(this.apiUrl, request);
  }

  /**
   * Get conversation between two users
   */
  getConversation(userId1: number, userId2: number): Observable<PrivateMessage[]> {
    return this.http.get<PrivateMessage[]>(`${this.apiUrl}/conversation/${userId1}/${userId2}`);
  }

  /**
   * Get all conversations for a user
   */
  getUserConversations(userId: number): Observable<PrivateMessage[]> {
    return this.http.get<PrivateMessage[]>(`${this.apiUrl}/conversations/${userId}`);
  }

  /**
   * Get unread messages for a user
   */
  getUnreadMessages(userId: number): Observable<PrivateMessage[]> {
    return this.http.get<PrivateMessage[]>(`${this.apiUrl}/unread/${userId}`);
  }

  /**
   * Count unread messages
   */
  countUnreadMessages(userId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/unread/${userId}/count`);
  }

  /**
   * Mark message as read
   */
  markAsRead(messageId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${messageId}/read`, {});
  }

  /**
   * Mark all messages in conversation as read
   */
  markConversationAsRead(userId: number, otherUserId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/conversation/${userId}/${otherUserId}/read`, {});
  }

  /**
   * Delete message for sender
   */
  deleteForSender(messageId: number, senderId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${messageId}/sender/${senderId}`);
  }

  /**
   * Delete message for receiver
   */
  deleteForReceiver(messageId: number, receiverId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${messageId}/receiver/${receiverId}`);
  }

  /**
   * Add reaction to message
   */
  addReaction(messageId: number, emoji: string): Observable<PrivateMessage> {
    return this.http.post<PrivateMessage>(`${this.apiUrl}/${messageId}/reaction`, { emoji });
  }

  /**
   * Remove reaction from message
   */
  removeReaction(messageId: number): Observable<PrivateMessage> {
    return this.http.delete<PrivateMessage>(`${this.apiUrl}/${messageId}/reaction`);
  }
}
