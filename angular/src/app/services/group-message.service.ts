import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export enum MessageType {
  TEXT = 'TEXT',
  IMAGE = 'IMAGE',
  GIF = 'GIF',
  FILE = 'FILE',
  EMOJI = 'EMOJI',
  SYSTEM = 'SYSTEM'
}

export interface GroupMessage {
  id?: number;
  groupId?: number;
  senderId: number;
  senderName: string;
  senderAvatar?: string;
  content: string;
  type: MessageType;
  mediaUrl?: string;
  gifUrl?: string;
  sentAt?: Date;
  editedAt?: Date;
  isEdited?: boolean;
  isDeleted?: boolean;
  replyToMessageId?: number;
  reactions?: string;
  aiSummary?: string;
  hasSummary?: boolean;
  audioData?: string;
  audioDuration?: number;
}

export interface GroupMessageRequest {
  groupId?: number;
  senderId: number;
  senderName: string;
  senderAvatar?: string;
  content: string;
  type?: MessageType;
  mediaUrl?: string;
  gifUrl?: string;
  replyToMessageId?: number;
  audioData?: string;
  audioDuration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class GroupMessageService {
  private apiUrl = `${environment.apiUrl}/groups`;

  constructor(private http: HttpClient) {}

  /**
   * Send a message to a group
   */
  sendMessage(groupId: number, request: GroupMessageRequest): Observable<GroupMessage> {
    return this.http.post<GroupMessage>(`${this.apiUrl}/${groupId}/messages`, request);
  }

  /**
   * Get messages for a group
   */
  getGroupMessages(groupId: number): Observable<GroupMessage[]> {
    return this.http.get<GroupMessage[]>(`${this.apiUrl}/${groupId}/messages`);
  }

  /**
   * Get recent messages for a group
   */
  getRecentMessages(groupId: number, limit: number = 50): Observable<GroupMessage[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<GroupMessage[]>(`${this.apiUrl}/${groupId}/messages/recent`, { params });
  }

  /**
   * Get messages after a certain time (for real-time updates)
   */
  getMessagesAfter(groupId: number, after: Date): Observable<GroupMessage[]> {
    const params = new HttpParams().set('after', after.toISOString());
    return this.http.get<GroupMessage[]>(`${this.apiUrl}/${groupId}/messages/after`, { params });
  }

  /**
   * Get message by ID
   */
  getMessageById(messageId: number): Observable<GroupMessage> {
    return this.http.get<GroupMessage>(`${this.apiUrl}/messages/${messageId}`);
  }

  /**
   * Edit a message
   */
  editMessage(messageId: number, newContent: string): Observable<GroupMessage> {
    return this.http.put<GroupMessage>(`${this.apiUrl}/messages/${messageId}`, { content: newContent });
  }

  /**
   * Delete a message
   */
  deleteMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/messages/${messageId}`);
  }

  /**
   * Add reaction to a message
   */
  addReaction(messageId: number, userId: number, emoji: string): Observable<GroupMessage> {
    return this.http.post<GroupMessage>(`${this.apiUrl}/messages/${messageId}/reaction`, {
      userId,
      emoji
    });
  }

  /**
   * Generate AI summary for a message
   */
  generateSummary(messageId: number): Observable<{ summary: string }> {
    return this.http.post<{ summary: string }>(`${this.apiUrl}/messages/${messageId}/summary`, {});
  }

  /**
   * Batch generate summaries for all long messages in a group
   */
  generateSummariesForGroup(groupId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/${groupId}/messages/generate-summaries`, {});
  }

  /**
   * Count messages in group
   */
  countMessages(groupId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/${groupId}/messages/count`);
  }
}
