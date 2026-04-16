// ── Forum Post ──────────────────────────────────────────────────────────────
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

// ── Trending Topic ───────────────────────────────────────────────────────────
export interface TrendingTopic {
  id?: number;
  category?: string;
  title: string;
  isPinned?: boolean;
  viewCount?: number;
  postCount?: number;
  createdAt?: string | Date;
  updatedAt?: string | Date;
}

// ── Notification ─────────────────────────────────────────────────────────────
export interface ForumNotification {
  id?: number;
  userId?: number;
  postId?: number;
  fromUserId?: number;
  fromUsername?: string;
  fromAvatar?: string;
  message?: string;
  type?: 'TAG' | 'REPLY' | 'REPORT_UPDATE';
  isRead?: boolean;
  createdAt?: string | Date;
}

// ── Friendship ───────────────────────────────────────────────────────────────
export interface Friendship {
  id?: number;
  userId1?: number;
  userId2?: number;
  status?: 'PENDING' | 'ACCEPTED' | 'BLOCKED';
  createdAt?: string | Date;
  updatedAt?: string | Date;
}

// ── Chat Message ─────────────────────────────────────────────────────────────
export interface ChatMessage {
  id?: number;
  senderId?: number;
  senderName?: string;
  senderAvatar?: string;
  receiverId?: number;
  receiverName?: string;
  content?: string;
  messageType?: 'TEXT' | 'IMAGE' | 'GIF' | 'SHARED_POST';
  imageUrl?: string;
  gifUrl?: string;
  sharedPostId?: number;
  replyToId?: number;
  replyToContent?: string;
  reactions?: Record<string, string>;
  isRead?: boolean;
  readAt?: string | Date;
  isForwarded?: boolean;
  forwardedFromName?: string;
  createdAt?: string | Date;
}

// ── User Status ───────────────────────────────────────────────────────────────
export interface UserStatus {
  id?: number;
  userId: number;
  status: 'ONLINE' | 'OFFLINE';
  lastSeen?: string | Date;
}

// ── Forum Report ──────────────────────────────────────────────────────────────
export interface ForumReport {
  id?: number;
  postId?: number;
  reporterId?: number;
  reportedUserId?: number;
  reason?: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  description?: string;
  createdAt?: string | Date;
  updatedAt?: string | Date;
}
