import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ForumPost } from '../models/forum.model';

@Injectable({ providedIn: 'root' })
export class ForumApiService {
  private readonly base = '/api/forums';

  constructor(private http: HttpClient) {}

  getTopLevelPosts(): Observable<ForumPost[]> {
    return this.http.get<ForumPost[]>(`${this.base}/get-top-level-forums`);
  }

  getReplies(postId: number): Observable<ForumPost[]> {
    return this.http.get<ForumPost[]>(`${this.base}/get-replies/${postId}`);
  }

  createPost(post: ForumPost): Observable<ForumPost> {
    return this.http.post<ForumPost>(`${this.base}/create-forum`, post);
  }

  likePost(id: number): Observable<ForumPost> {
    return this.http.put<ForumPost>(`${this.base}/like-forum/${id}`, {});
  }
}
