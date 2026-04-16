import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

interface Post {
  id: number;
  content: string;
  createdAt: Date;
  likes: number;
  comments: number;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  // User data
  currentUserId: number = 1;
  currentUserName: string = 'Test User';
  currentUserRole: string = 'Freelancer';
  currentUserBio: string = 'Passionate freelancer specializing in web development and design.';
  
  // Cover and avatar
  coverImage: string = '';
  avatarImage: string = '';
  
  // Stats
  subscriberCount: number = 0;
  subscriptionCount: number = 0;
  postCount: number = 0;
  
  // Tabs
  activeTab: string = 'all';
  
  // Posts
  userPosts: Post[] = [];
  
  // Edit mode
  isEditingProfile: boolean = false;
  editName: string = '';
  editBio: string = '';

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.loadUserData();
    this.loadUserPosts();
    this.loadUserStats();
  }

  loadUserData(): void {
    // TODO: Load from User microservice
    this.editName = this.currentUserName;
    this.editBio = this.currentUserBio;
    console.log('✅ User data loaded');
  }

  loadUserPosts(): void {
    // TODO: Load user's posts from backend
    // For now, load from localStorage threads
    const storedThreads = localStorage.getItem('threads');
    if (storedThreads) {
      const allThreads = JSON.parse(storedThreads);
      this.userPosts = allThreads
        .filter((t: any) => t.author === this.currentUserName)
        .map((t: any) => ({
          id: t.id || t.forumPostId,
          content: t.content,
          createdAt: new Date(t.createdAt),
          likes: t.likes || 0,
          comments: t.postCount || 0
        }));
    }
    console.log('✅ User posts loaded:', this.userPosts.length);
  }

  loadUserStats(): void {
    // TODO: Load from backend
    this.postCount = this.userPosts.length;
    this.subscriberCount = 0; // Placeholder
    this.subscriptionCount = 0; // Placeholder
    console.log('✅ User stats loaded');
  }

  goBack(): void {
    this.router.navigate(['/threads']);
  }

  startEditProfile(): void {
    this.isEditingProfile = true;
  }

  cancelEditProfile(): void {
    this.isEditingProfile = false;
    this.editName = this.currentUserName;
    this.editBio = this.currentUserBio;
  }

  saveProfile(): void {
    // TODO: Save to backend
    this.currentUserName = this.editName;
    this.currentUserBio = this.editBio;
    this.isEditingProfile = false;
    console.log('✅ Profile saved');
  }

  onCoverImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.coverImage = e.target?.result as string;
      };
      reader.readAsDataURL(input.files[0]);
    }
  }

  onAvatarImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.avatarImage = e.target?.result as string;
      };
      reader.readAsDataURL(input.files[0]);
    }
  }

  getInitials(name: string | null): string {
    if (!name) return 'U';
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  formatTime(date: Date): string {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 7) return `${diffDays}d ago`;
    
    return date.toLocaleDateString();
  }

  selectTab(tab: string): void {
    this.activeTab = tab;
  }
}
