export interface ChatMessage {
  id: string;
  fromUserId: number;
  toUserId: number;
  text: string;
  createdAt: string; 
}

export interface ConversationPreview {
  otherUserId: number;
  otherUserName: string;
  lastMessage: string;
  lastAt: string;
  unreadCount?: number;
}
