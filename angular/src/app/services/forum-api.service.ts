import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ForumPost,
  TrendingTopic,
  ForumNotification,
  Friendship,
  ChatMessage,
  UserStatus,
  ForumReport,
} from '../models/forum.model';

@Injectable({ providedIn: 'root' })
export class ForumApiService {
  private base = 'http://localhost:8082/api/forums';

  constructor(private http: HttpClient) {}

  // ── Posts ──────────────────────────────────────────────────────────────────

  getAllPosts(): Observable<ForumPost[]> {
    return this.http.get<ForumPost[]>(`${this.base}/get-all-forums`);
  }

  getTopLevelPosts(): Observable<ForumPost[]> {
    return this.http.get<ForumPost[]>(`${this.base}/get-top-level-forums`);
  }

  getPostById(id: number): Observable<ForumPost> {
    return this.http.get<ForumPost>(`${this.base}/get-forum-by-id/${id}`);
  }

  getPostsByTopic(topicId: number): Observable<ForumPost[]> {
    return this.http.get<ForumPost[]>(`${this.base}/get-forums-by-topic/${topicId}`);
  }

  getPostsByUser(userId: number): Observable<ForumPost[]> {
    return this.http.get<ForumPost[]>(`${this.base}/get-forums-by-user/${userId}`);
  }

  getReplies(postId: number): Observable<ForumPost[]> {
    return this.http.get<ForumPost[]>(`${this.base}/get-replies/${postId}`);
  }

  createPost(post: ForumPost): Observable<ForumPost> {
    return this.http.post<ForumPost>(`${this.base}/create-forum`, post);
  }

  updatePost(id: number, post: ForumPost): Observable<ForumPost> {
    return this.http.put<ForumPost>(`${this.base}/update-forum/${id}`, post);
  }

  deletePost(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/delete-forum/${id}`);
  }

  likePost(id: number): Observable<ForumPost> {
    return this.http.put<ForumPost>(`${this.base}/like-forum/${id}`, {});
  }

  unlikePost(id: number): Observable<ForumPost> {
    return this.http.put<ForumPost>(`${this.base}/unlike-forum/${id}`, {});
  }

  repostPost(id: number): Observable<ForumPost> {
    return this.http.put<ForumPost>(`${this.base}/repost-forum/${id}`, {});
  }

  // ── Trending Topics ────────────────────────────────────────────────────────

  getTrendingTopics(): Observable<TrendingTopic[]> {
    return this.http.get<TrendingTopic[]>(`${this.base}/get-trending-topics`);
  }

  getAllTopics(): Observable<TrendingTopic[]> {
    return this.http.get<TrendingTopic[]>(`${this.base}/get-all-topics`);
  }

  getPinnedTopics(): Observable<TrendingTopic[]> {
    return this.http.get<TrendingTopic[]>(`${this.base}/get-pinned-topics`);
  }

  incrementTopicView(id: number): Observable<TrendingTopic> {
    return this.http.put<TrendingTopic>(`${this.base}/increment-topic-view-count/${id}`, {});
  }

  // ── Notifications ──────────────────────────────────────────────────────────

  getNotifications(userId: number): Observable<ForumNotification[]> {
    return this.http.get<ForumNotification[]>(`${this.base}/get-notifications/${userId}`);
  }

  getUnreadNotifications(userId: number): Observable<ForumNotification[]> {
    return this.http.get<ForumNotification[]>(`${this.base}/get-unread-notifications/${userId}`);
  }

  getUnreadNotificationCount(userId: number): Observable<number> {
    return this.http.get<number>(`${this.base}/get-unread-count/${userId}`);
  }

  markNotificationRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.base}/mark-notification-read/${id}`, {});
  }

  markAllNotificationsRead(userId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/mark-all-notifications-read/${userId}`, {});
  }

  // ── Friendships ────────────────────────────────────────────────────────────

  sendFriendRequest(friendship: Friendship): Observable<Friendship> {
    return this.http.post<Friendship>(`${this.base}/send-friend-request`, friendship);
  }

  acceptFriendRequest(id: number): Observable<Friendship> {
    return this.http.put<Friendship>(`${this.base}/accept-friend-request/${id}`, {});
  }

  rejectFriendRequest(id: number): Observable<Friendship> {
    return this.http.put<Friendship>(`${this.base}/reject-friend-request/${id}`, {});
  }

  removeFriend(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/remove-friend/${id}`);
  }

  getFriends(userId: number): Observable<Friendship[]> {
    return this.http.get<Friendship[]>(`${this.base}/get-friends/${userId}`);
  }

  getPendingRequests(userId: number): Observable<Friendship[]> {
    return this.http.get<Friendship[]>(`${this.base}/get-pending-requests/${userId}`);
  }

  getFriendshipStatus(userId1: number, userId2: number): Observable<Friendship> {
    return this.http.get<Friendship>(`${this.base}/get-friendship-status/${userId1}/${userId2}`);
  }

  // ── Chat ───────────────────────────────────────────────────────────────────

  sendMessage(message: ChatMessage): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base}/send-message`, message);
  }

  getConversation(userId1: number, userId2: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.base}/get-conversation/${userId1}/${userId2}`);
  }

  getUnreadMessages(userId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.base}/get-unread-messages/${userId}`);
  }

  markConversationRead(senderId: number, receiverId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/mark-conversation-read/${senderId}/${receiverId}`, {});
  }

  // ── User Status ────────────────────────────────────────────────────────────

  sendHeartbeat(userId: number): Observable<UserStatus> {
    return this.http.post<UserStatus>(`${this.base}/user-heartbeat`, { userId });
  }

  setUserOffline(userId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/user-offline/${userId}`, {});
  }

  getUserStatus(userId: number): Observable<UserStatus> {
    return this.http.get<UserStatus>(`${this.base}/user-status/${userId}`);
  }

  getUserStatuses(userIds: number[]): Observable<UserStatus[]> {
    return this.http.post<UserStatus[]>(`${this.base}/user-statuses`, userIds);
  }

  // ── Reports ────────────────────────────────────────────────────────────────

  createReport(report: ForumReport): Observable<ForumReport> {
    return this.http.post<ForumReport>(`${this.base}/create-report`, report);
  }

  // ── AI Correction ──────────────────────────────────────────────────────────

  correctText(text: string): Observable<any> {
    // DISABLED - Use direct Gemini API call instead
    console.error('❌ OLD correctText method called - this should not happen!');
    throw new Error('Use callGeminiDirectlyFIXED instead');
  }
}
