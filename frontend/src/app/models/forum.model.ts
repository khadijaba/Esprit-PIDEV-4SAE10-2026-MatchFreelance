export interface ForumPost {
  id?: number;
  topicId?: number;
  userId?: number;
  author?: string;
  username?: string;
  avatar?: string;
  content: string;
  image?: string;
  isEdited?: boolean;
  parentPostId?: number;
  sharedPostId?: number;
  comments?: number;
  reposts?: number;
  likes?: number;
  dislikes?: number;
  createdAt?: string | Date;
  updatedAt?: string | Date;
}

export interface ForumChatMessage {
  id?: number;
  senderId: number;
  senderName?: string;
  senderAvatar?: string;
  receiverId: number;
  receiverName?: string;
  content: string;
  messageType?: 'TEXT' | 'IMAGE' | 'GIF' | 'SHARED_POST';
  imageUrl?: string;
  gifUrl?: string;
  sharedPostId?: number;
  isRead?: boolean;
  createdAt?: string | Date;
}
