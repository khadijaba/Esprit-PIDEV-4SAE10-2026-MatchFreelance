import { Injectable } from '@angular/core';
import { ChatMessage } from '../models/chat.model';

const STORAGE_PREFIX = 'matchfreelance_chat_';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private conversationKey(userId1: number, userId2: number): string {
    const a = Math.min(userId1, userId2);
    const b = Math.max(userId1, userId2);
    return `${STORAGE_PREFIX}${a}_${b}`;
  }

  getMessages(userId1: number, userId2: number): ChatMessage[] {
    const key = this.conversationKey(userId1, userId2);
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return [];
      const arr = JSON.parse(raw) as ChatMessage[];
      return Array.isArray(arr) ? arr.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()) : [];
    } catch {
      return [];
    }
  }

  sendMessage(fromUserId: number, toUserId: number, text: string): ChatMessage {
    const messages = this.getMessages(fromUserId, toUserId);
    const msg: ChatMessage = {
      id: `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`,
      fromUserId,
      toUserId,
      text: text.trim(),
      createdAt: new Date().toISOString(),
    };
    messages.push(msg);
    const key = this.conversationKey(fromUserId, toUserId);
    localStorage.setItem(key, JSON.stringify(messages));
    return msg;
  }

  /** Liste des IDs des utilisateurs avec qui l'utilisateur courant a une conversation */
  getConversationPartnerIds(currentUserId: number): number[] {
    const keys: number[] = [];
    const prefix = STORAGE_PREFIX;
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (!key?.startsWith(prefix)) continue;
      const rest = key.slice(prefix.length);
      const parts = rest.split('_').map(Number);
      if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
        const other = parts[0] === currentUserId ? parts[1] : parts[1] === currentUserId ? parts[0] : null;
        if (other != null && !keys.includes(other)) keys.push(other);
      }
    }
    return keys;
  }
}
