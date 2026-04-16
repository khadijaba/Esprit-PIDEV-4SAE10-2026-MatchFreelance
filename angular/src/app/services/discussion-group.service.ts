import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface DiscussionGroup {
  id?: number;
  name: string;
  description?: string;
  topic: string;
  logoUrl?: string;
  creatorId: number;
  creatorName: string;
  createdAt?: Date;
  updatedAt?: Date;
  isPrivate: boolean;
  isActive?: boolean;
  memberCount?: number;
  messageCount?: number;
  lastActivityAt?: Date;
  allowMemberInvites?: boolean;
  allowFileSharing?: boolean;
  allowGifs?: boolean;
  allowEmojis?: boolean;
}

export interface DiscussionGroupRequest {
  name: string;
  description?: string;
  topic: string;
  logoUrl?: string;
  creatorId: number;
  creatorName: string;
  isPrivate?: boolean;
  allowMemberInvites?: boolean;
  allowFileSharing?: boolean;
  allowGifs?: boolean;
  allowEmojis?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class DiscussionGroupService {
  private apiUrl = `${environment.apiUrl}/groups`;

  constructor(private http: HttpClient) {}

  /**
   * Create a new discussion group
   */
  createGroup(request: DiscussionGroupRequest): Observable<DiscussionGroup> {
    return this.http.post<DiscussionGroup>(this.apiUrl, request);
  }

  /**
   * Get all active groups
   */
  getAllGroups(): Observable<DiscussionGroup[]> {
    return this.http.get<DiscussionGroup[]>(this.apiUrl);
  }

  /**
   * Get public groups
   */
  getPublicGroups(): Observable<DiscussionGroup[]> {
    return this.http.get<DiscussionGroup[]>(`${this.apiUrl}/public`);
  }

  /**
   * Get group by ID
   */
  getGroupById(id: number): Observable<DiscussionGroup> {
    return this.http.get<DiscussionGroup>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get groups by topic
   */
  getGroupsByTopic(topic: string): Observable<DiscussionGroup[]> {
    return this.http.get<DiscussionGroup[]>(`${this.apiUrl}/topic/${topic}`);
  }

  /**
   * Get groups created by user
   */
  getGroupsByCreator(creatorId: number): Observable<DiscussionGroup[]> {
    return this.http.get<DiscussionGroup[]>(`${this.apiUrl}/creator/${creatorId}`);
  }

  /**
   * Get user's groups (where user is a member)
   */
  getUserGroups(userId: number): Observable<DiscussionGroup[]> {
    return this.http.get<DiscussionGroup[]>(`${this.apiUrl}/user/${userId}`);
  }

  /**
   * Update group
   */
  updateGroup(id: number, request: Partial<DiscussionGroupRequest>): Observable<DiscussionGroup> {
    return this.http.put<DiscussionGroup>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete group
   */
  deleteGroup(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Search groups by name
   */
  searchGroups(searchTerm: string): Observable<DiscussionGroup[]> {
    return this.http.get<DiscussionGroup[]>(`${this.apiUrl}/search`, {
      params: { q: searchTerm }
    });
  }

  /**
   * Get popular groups
   */
  getPopularGroups(): Observable<DiscussionGroup[]> {
    return this.http.get<DiscussionGroup[]>(`${this.apiUrl}/popular`);
  }

  /**
   * Leave a group (remove membership)
   */
  leaveGroup(groupId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${groupId}/members/${userId}`);
  }

  /**
   * Report a message in a group
   */
  reportMessage(groupId: number, messageId: number, report: { reporterId: number; reason: string; description?: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/${groupId}/messages/${messageId}/report`, report);
  }
}
