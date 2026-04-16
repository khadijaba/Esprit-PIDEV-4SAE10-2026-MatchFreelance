import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export enum FriendRequestStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED'
}

export interface FriendRequest {
  id?: number;
  senderId: number;
  receiverId: number;
  senderName: string;
  receiverName: string;
  senderAvatar?: string;
  status: FriendRequestStatus;
  sentAt?: Date;
  respondedAt?: Date;
  message?: string;
}

export interface FriendRequestDto {
  senderId: number;
  receiverId: number;
  senderName: string;
  receiverName: string;
  senderAvatar?: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class FriendRequestService {
  private apiUrl = `${environment.apiUrl}/friends`;

  constructor(private http: HttpClient) {}

  /**
   * Send a friend request
   */
  sendFriendRequest(dto: FriendRequestDto): Observable<FriendRequest> {
    return this.http.post<FriendRequest>(`${this.apiUrl}/request`, dto);
  }

  /**
   * Get pending requests received by user
   */
  getPendingRequestsReceived(userId: number): Observable<FriendRequest[]> {
    return this.http.get<FriendRequest[]>(`${this.apiUrl}/requests/received/${userId}`);
  }

  /**
   * Get pending requests sent by user
   */
  getPendingRequestsSent(userId: number): Observable<FriendRequest[]> {
    return this.http.get<FriendRequest[]>(`${this.apiUrl}/requests/sent/${userId}`);
  }

  /**
   * Accept friend request
   */
  acceptFriendRequest(requestId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/requests/${requestId}/accept`, {});
  }

  /**
   * Reject friend request
   */
  rejectFriendRequest(requestId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/requests/${requestId}/reject`, {});
  }

  /**
   * Cancel friend request
   */
  cancelFriendRequest(requestId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/requests/${requestId}`);
  }

  /**
   * Count pending requests for user
   */
  countPendingRequests(userId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/requests/received/${userId}/count`);
  }
}
