import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Thread {
  id?: number;
  title: string;
  content: string;
  author: string;
  authorRole: string;
  authorAvatar?: string;
  createdAt?: string | Date;
  postCount?: number;
  likes: number;
  retweets: number;
  tags: string[];
  category: string;
}

export interface Comment {
  id?: number;
  author: string;
  authorRole: string;
  content: string;
  createdAt?: string | Date;
  likes: number;
}

@Injectable({
  providedIn: 'root'
})
export class ThreadApiService {
  private apiUrl = 'http://localhost:8082/api/threads';

  constructor(private http: HttpClient) {}

  getThreads(category?: string): Observable<Thread[]> {
    let url = this.apiUrl;
    if (category && category !== 'all') {
      url += `?category=${category}`;
    }
    return this.http.get<Thread[]>(url);
  }

  createThread(thread: Thread): Observable<Thread> {
    return this.http.post<Thread>(this.apiUrl, thread);
  }

  getComments(threadId: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.apiUrl}/${threadId}/comments`);
  }

  addComment(threadId: number, comment: Comment): Observable<Comment> {
    return this.http.post<Comment>(`${this.apiUrl}/${threadId}/comments`, comment);
  }

  toggleLike(threadId: number, increment: boolean): Observable<Thread> {
    return this.http.post<Thread>(`${this.apiUrl}/${threadId}/like?increment=${increment}`, {});
  }

  toggleRetweet(threadId: number, increment: boolean): Observable<Thread> {
    return this.http.post<Thread>(`${this.apiUrl}/${threadId}/retweet?increment=${increment}`, {});
  }
}
