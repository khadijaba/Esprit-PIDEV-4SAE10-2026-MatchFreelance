import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ForumApiService } from '../../services/forum-api.service';
import { ForumPost } from '../../models/forum.model';
import { AuthService } from '../../services/auth.service';

/**
 * Forum/blog natif dans validation (sans dépendance au frontend 4201).
 */
@Component({
  selector: 'app-blog-forum-embed',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './blog-forum-embed.component.html',
  styleUrl: './blog-forum-embed.component.css',
})
export class BlogForumEmbedComponent implements OnInit {
  posts: ForumPost[] = [];
  repliesByPostId: Record<number, ForumPost[]> = {};
  replyContentByPostId: Record<number, string> = {};
  newPostContent = '';
  loading = false;
  creating = false;
  activeNav = 'for-you';
  categories = [
    { value: 'for-you', label: 'For you', color: 'purple' },
    { value: 'following', label: 'Following', color: 'green' },
    { value: 'trending', label: 'Trending', color: 'orange' },
  ];

  ngOnInit(): void {
    this.loadPosts();
  }

  constructor(
    private auth: AuthService,
    private forumApi: ForumApiService
  ) {}

  get currentUserName(): string {
    const u = this.auth.getStoredUser();
    return u?.fullName ?? u?.email ?? 'Guest User';
  }

  get currentUserRole(): string {
    return this.auth.getStoredUser()?.role ?? 'USER';
  }

  loadPosts(): void {
    this.loading = true;
    this.forumApi.getTopLevelPosts().subscribe({
      next: (rows) => {
        this.posts = rows ?? [];
        this.loading = false;
      },
      error: () => {
        this.posts = [];
        this.loading = false;
      },
    });
  }

  createPost(): void {
    const user = this.auth.getStoredUser();
    const content = this.newPostContent.trim();
    if (!user?.userId || !content) return;

    this.creating = true;
    this.forumApi
      .createPost({
        userId: user.userId,
        content,
        author: user.fullName ?? user.email,
        username: user.email,
      })
      .subscribe({
        next: (created) => {
          this.posts = [created, ...this.posts];
          this.newPostContent = '';
          this.creating = false;
        },
        error: () => {
          this.creating = false;
        },
      });
  }

  like(post: ForumPost): void {
    if (!post.id) return;
    this.forumApi.likePost(post.id).subscribe({
      next: (updated) => {
        this.posts = this.posts.map((p) => (p.id === updated.id ? updated : p));
      },
    });
  }

  toggleReplies(post: ForumPost): void {
    if (!post.id) return;
    if (this.repliesByPostId[post.id]) {
      delete this.repliesByPostId[post.id];
      return;
    }
    this.forumApi.getReplies(post.id).subscribe({
      next: (rows) => (this.repliesByPostId[post.id!] = rows ?? []),
      error: () => (this.repliesByPostId[post.id!] = []),
    });
  }

  submitReply(post: ForumPost): void {
    if (!post.id) return;
    const user = this.auth.getStoredUser();
    const content = (this.replyContentByPostId[post.id] ?? '').trim();
    if (!user?.userId || !content) return;

    this.forumApi
      .createPost({
        userId: user.userId,
        parentPostId: post.id,
        content,
        author: user.fullName ?? user.email,
        username: user.email,
      })
      .subscribe({
        next: (created) => {
          const current = this.repliesByPostId[post.id!] ?? [];
          this.repliesByPostId[post.id!] = [...current, created];
          this.replyContentByPostId[post.id!] = '';
        },
      });
  }

  asDate(value?: string | Date): string {
    if (!value) return '';
    const d = typeof value === 'string' ? new Date(value) : value;
    if (Number.isNaN(d.getTime())) return '';
    return d.toLocaleString();
  }

  getInitials(name?: string | null): string {
    const n = (name ?? '').trim();
    if (!n) return 'U';
    const parts = n.split(/\s+/).filter(Boolean);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  selectNav(value: string): void {
    this.activeNav = value;
  }
}
