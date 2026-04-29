import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ForumApiService } from '../../services/forum-api.service';
import { ForumChatMessage, ForumPost } from '../../models/forum.model';
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
  spaces = ['All', 'Freelancer hub', 'Project owners', 'Open collab', 'Skill exchange', 'Jobs & Opportunities'];
  selectedSpace = 'All';

  isThreadDrawerOpen = false;
  isGroupModalOpen = false;
  threadDraft = '';
  threadTagDraft = '';

  groups: { name: string; topic: string; isPrivate: boolean }[] = [];
  groupNameDraft = '';
  groupTopicDraft = 'Freelancing';
  groupPrivateDraft = false;
  allowInvites = true;
  allowEmojis = true;
  allowGifs = true;

  sessionUsers: Array<{ userId: number; fullName: string; email: string; role: string }> = [];
  selectedSessionUserId: number | null = null;
  conversation: ForumChatMessage[] = [];
  messageDraft = '';
  loadingConversation = false;

  ngOnInit(): void {
    this.loadPosts();
    this.loadSessionUsers();
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

  get currentUserId(): number | null {
    return this.auth.getStoredUser()?.userId ?? null;
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

  createPost(contentOverride?: string): void {
    const user = this.auth.getStoredUser();
    const content = (contentOverride ?? this.newPostContent).trim();
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
          this.threadDraft = '';
          this.threadTagDraft = '';
          this.creating = false;
          this.isThreadDrawerOpen = false;
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

  loadSessionUsers(): void {
    const current = this.currentUserId;
    if (!current) return;
    this.auth.getUsers().subscribe({
      next: (users) => {
        this.sessionUsers = (users ?? [])
          .filter((u) => u.id !== current)
          .map((u) => ({
            userId: u.id,
            fullName: u.fullName ?? u.email,
            email: u.email,
            role: u.role,
          }));
        if (this.sessionUsers.length > 0) {
          this.openSession(this.sessionUsers[0].userId);
        }
      },
      error: () => {
        this.sessionUsers = [];
      },
    });
  }

  openSession(otherUserId: number): void {
    const current = this.currentUserId;
    if (!current) return;
    this.selectedSessionUserId = otherUserId;
    this.loadingConversation = true;
    this.forumApi.getConversation(current, otherUserId).subscribe({
      next: (rows) => {
        this.conversation = rows ?? [];
        this.loadingConversation = false;
      },
      error: () => {
        this.conversation = [];
        this.loadingConversation = false;
      },
    });
  }

  sendMessage(): void {
    const current = this.currentUserId;
    const receiverId = this.selectedSessionUserId;
    const content = this.messageDraft.trim();
    if (!current || !receiverId || !content) return;
    const selectedUser = this.sessionUsers.find((u) => u.userId === receiverId);
    this.forumApi
      .sendDirectMessage({
        senderId: current,
        senderName: this.currentUserName,
        receiverId,
        receiverName: selectedUser?.fullName,
        content,
        messageType: 'TEXT',
      })
      .subscribe({
        next: (created) => {
          this.conversation = [...this.conversation, created];
          this.messageDraft = '';
        },
      });
  }

  openThreadDrawer(): void {
    this.isThreadDrawerOpen = true;
  }

  closeThreadDrawer(): void {
    this.isThreadDrawerOpen = false;
  }

  openGroupModal(): void {
    this.isGroupModalOpen = true;
  }

  closeGroupModal(): void {
    this.isGroupModalOpen = false;
  }

  submitThreadFromDrawer(): void {
    this.createPost(this.threadDraft);
  }

  createGroup(): void {
    const name = this.groupNameDraft.trim();
    if (!name) return;
    this.groups = [
      {
        name,
        topic: this.groupTopicDraft,
        isPrivate: this.groupPrivateDraft,
      },
      ...this.groups,
    ];
    this.groupNameDraft = '';
    this.groupTopicDraft = 'Freelancing';
    this.groupPrivateDraft = false;
    this.allowInvites = true;
    this.allowEmojis = true;
    this.allowGifs = true;
    this.isGroupModalOpen = false;
  }
}
