import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ToxicityApiService } from '../../services/toxicity-api.service';
import { ThreadApiService, Thread as ApiThread, Comment as ApiComment } from '../../services/thread-api.service';
import { ForumApiService } from '../../services/forum-api.service';
import { ForumPost, TrendingTopic as ForumTopic, ForumNotification, ForumReport } from '../../models/forum.model';
import { NotificationService } from '../../services/notification.service';
import { NotificationComponent } from '../notification/notification.component';
import { StorageCleanupService } from '../../services/storage-cleanup.service';
import { DiscussionGroupService, DiscussionGroup } from '../../services/discussion-group.service';
import { GroupMessageService, MessageType } from '../../services/group-message.service';
import { FriendRequestService, FriendRequest } from '../../services/friend-request.service';

interface Thread {
  id?: number;
  title: string;
  content: string;
  author: string;
  authorRole: string;
  authorAvatar?: string;
  createdAt: Date;
  postCount?: number;
  likes: number;
  dislikes: number;
  retweets: number;
  tags: string[];
  category: string;
  // Forum-extended fields
  forumPostId?: number;   // id in the forum backend
  image?: string;
  isEdited?: boolean;
  userId?: number;
  reposts?: number;
  parentPostId?: number;
  sharedPostId?: number;
}

// Banned words regex available globally for component sync check
// Hardened to be more inclusive of common variations
const BANNED_WORDS_REGEX = /\b(fuck|shit|asshole|bitch|bastard|dick|pussy|slut|whore|crap|damn|hell)\b/i;

interface TrendingTag {
  name: string;
  postsCount: string;
}

interface WhoToFollow {
  name: string;
  role: string;
  avatar: string;
  initials: string;
  isFollowing: boolean;
}

@Component({
  selector: 'app-threads',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NotificationComponent],
  templateUrl: './threads.component.html',
  styleUrl: './threads.component.css',
})
export class ThreadsComponent implements OnInit, OnDestroy {
  threads: Thread[] = [];
  selectedCategory = 'all';
  activeTab = 'for-you';
  activeNav = 'for-you';
  newPostContent = '';
  newPostCategory = 'Freelancer hub';
  selectedTag: string | null = null;
  followedAuthors: string[] = [];
  likedThreads: number[] = [];
  dislikedThreads: number[] = [];
  retweetedThreads: number[] = [];

  trendingTags: TrendingTag[] = [
    // Static tags removed - will be populated dynamically
  ];

  whoToFollow: WhoToFollow[] = [
    // Static users removed - will be populated from User microservice
  ];

  categories = [
    { value: 'all', label: 'All' },
    { value: 'Freelancer hub', label: 'Freelancer hub' },
    { value: 'Project owners', label: 'Project owners' },
    { value: 'Open collab', label: 'Open collab' },
    { value: 'Skill exchange', label: 'Skill exchange' },
    { value: 'jobs', label: 'Jobs & Opportunities' }
  ];

  // Drawer state
  isDrawerOpen = false;
  newThreadBody = '';
  newThreadCategory = 'Freelancer hub';
  newThreadTags: string[] = [];
  tagInput = '';

  // Thread UI state
  expandedThreadId: number | null = null;
  threadReplies: Record<number, ApiComment[]> = {};
  inlineReplyContent = '';

  // AI Moderation State
  public toxicityApiService = inject(ToxicityApiService);
  private http = inject(HttpClient);
  private threadApi = inject(ThreadApiService);
  private forumApi = inject(ForumApiService);
  private notificationService = inject(NotificationService);
  private storageCleanup = inject(StorageCleanupService);
  isCheckingContent = false;
  showToxicityPopup = false;
  toxicLabels: string[] = [];
  
  // AI Correction State
  isCorrectingText = false;

  // Forum-specific state
  forumPosts: ForumPost[] = [];
  forumReplies: Record<number, ForumPost[]> = {};
  likedForumPosts: number[] = [];
  dislikedForumPosts: number[] = [];
  repostedForumPosts: number[] = [];

  // Notifications
  notifications: ForumNotification[] = [];
  unreadNotificationCount = 0;
  showNotificationsPanel = false;

  // Edit post
  editingPostId: number | null = null;
  editingContent = '';

  // Report post
  reportingPostId: number | null = null;
  reportReason = '';
  reportDescription = '';

  // Image upload
  newPostImage: string | null = null;

  // Inline reply for forum posts
  forumInlineReplyContent = '';
  expandedForumPostId: number | null = null;

  // Heartbeat interval
  private heartbeatInterval: ReturnType<typeof setInterval> | null = null;
  
  // User data - will be populated from User microservice
  currentUserId: number | null = null;
  currentUserName: string | null = null;
  currentUserRole: string | null = null;

  // Discussion Groups
  userGroups: DiscussionGroup[] = [];
  allGroups: DiscussionGroup[] = [];
  popularGroups: DiscussionGroup[] = [];
  private discussionGroupService = inject(DiscussionGroupService);

  // Group Chat Modal
  showGroupChatModal: boolean = false;
  selectedGroupForChat: DiscussionGroup | null = null;
  groupMessages: any[] = [];
  groupMessageContent: string = '';
  sendingGroupMessage: boolean = false;
  showGroupEmojiPicker: boolean = false;
  private groupMessageService = inject(GroupMessageService);

  // Voice Recording
  isRecording: boolean = false;
  recordingTime: number = 0;
  recordingInterval: any = null;
  mediaRecorder: MediaRecorder | null = null;
  audioChunks: Blob[] = [];

  // Private Messages
  privateChats: any[] = [];
  unreadMessagesCount: number = 0;

  // Friend Requests
  friendRequests: any[] = [];
  friendRequestCount: number = 0;
  showFriendRequestsPanel: boolean = false;
  private friendRequestService = inject(FriendRequestService);

  // User Profile Stats
  userPostCount: number = 0;
  showProfilePanel: boolean = false;

  constructor(private router: Router) {}

  ngOnInit() {
    // Initialize user data from User microservice (to be implemented)
    this.initializeUserData();
    
    // Clear static posts from localStorage (cleanup for microservices architecture)
    this.clearStaticPosts();
    
    this.loadThreads();
    this.loadLikedThreads();
    this.loadDislikedThreads();
    this.loadRetweetedThreads();
    this.loadFollowedAuthors();
    this.loadForumPosts();
    this.loadForumTrendingTopics();
    this.loadNotifications();
    this.loadLikedForumPosts();
    this.loadDislikedForumPosts();
    this.loadRepostedForumPosts();
    
    // Only start heartbeat if user is authenticated
    if (this.currentUserId) {
      this.startHeartbeat();
      this.loadUserGroups();
      this.loadAllGroups();
      this.loadPopularGroups();
      this.loadPrivateChats();
      this.loadFriendRequests();
      this.calculateUserPostCount();
    }

    // No need to init AI model anymore - using API
  }

  ngOnDestroy() {
    if (this.heartbeatInterval && this.currentUserId) {
      clearInterval(this.heartbeatInterval);
      this.forumApi.setUserOffline(this.currentUserId).subscribe();
    }
  }

  // ── User Authentication Integration ───────────────────────────────────────
  
  /**
   * Initialize user data from User microservice
   * This method should be called when user authentication is available
   */
  private initializeUserData() {
    // TODO: Replace with actual User microservice integration
    // For now, use mock data for testing blog functionality
    
    // TEMPORARY: Mock user data for testing (remove when User microservice is integrated)
    this.currentUserId = 1;
    this.currentUserName = 'Test User';
    this.currentUserRole = 'Freelancer';
    
    console.log('⚠️ Using mock user data for testing - User microservice integration pending');
    
    // Example for future integration:
    // this.authService.getCurrentUser().subscribe(user => {
    //   this.currentUserId = user.id;
    //   this.currentUserName = user.firstName + ' ' + user.lastName;
    //   this.currentUserRole = user.role;
    //   this.startHeartbeat();
    // });
  }

  /**
   * Clear static posts from localStorage
   * This removes any hardcoded test posts for clean microservices architecture
   */
  private clearStaticPosts() {
    // TEMPORARILY DISABLED: Keep this disabled during testing
    // Uncomment when ready for production with User microservice
    // this.storageCleanup.clearStaticPosts();
    
    console.log('⚠️ Static posts cleanup disabled for testing');
  }

  /**
   * Set user data when received from User microservice
   * Call this method when user authentication is complete
   */
  public setUserData(userId: number, userName: string, userRole: string) {
    this.currentUserId = userId;
    this.currentUserName = userName;
    this.currentUserRole = userRole;
    
    // Start heartbeat and load user-specific data
    this.startHeartbeat();
    this.loadNotifications();
    this.loadUserGroups();
    this.updateWhoToFollow();
  }

  /**
   * Clear user data on logout
   */
  public clearUserData() {
    if (this.heartbeatInterval && this.currentUserId) {
      clearInterval(this.heartbeatInterval);
      this.forumApi.setUserOffline(this.currentUserId).subscribe();
    }
    
    this.currentUserId = null;
    this.currentUserName = null;
    this.currentUserRole = null;
    this.heartbeatInterval = null;
  }

  // ── Heartbeat ─────────────────────────────────────────────────────────────
  private startHeartbeat() {
    if (!this.currentUserId) {
      console.warn('Cannot start heartbeat: user not authenticated');
      return;
    }
    
    this.forumApi.sendHeartbeat(this.currentUserId).subscribe();
    this.heartbeatInterval = setInterval(() => {
      if (this.currentUserId) {
        this.forumApi.sendHeartbeat(this.currentUserId).subscribe();
      }
    }, 30000);
  }

  // ── Discussion Groups ─────────────────────────────────────────────────────
  
  /**
   * Load user's discussion groups
   */
  loadUserGroups() {
    if (!this.currentUserId) {
      console.warn('Cannot load groups: user not authenticated');
      return;
    }
    
    this.discussionGroupService.getUserGroups(this.currentUserId).subscribe({
      next: (groups) => {
        this.userGroups = groups;
        console.log('✅ Loaded user groups:', groups.length);
      },
      error: (err) => {
        console.error('❌ Failed to load user groups:', err);
        this.userGroups = [];
      }
    });
  }

  /**
   * Load all public groups
   */
  loadAllGroups() {
    this.discussionGroupService.getPublicGroups().subscribe({
      next: (groups) => {
        this.allGroups = groups;
        console.log('✅ Loaded all groups:', groups.length);
      },
      error: (err) => {
        console.error('❌ Failed to load all groups:', err);
        this.allGroups = [];
      }
    });
  }

  /**
   * Load popular groups
   */
  loadPopularGroups() {
    this.discussionGroupService.getPopularGroups().subscribe({
      next: (groups) => {
        this.popularGroups = groups.slice(0, 5);
        console.log('✅ Loaded popular groups:', groups.length);
      },
      error: (err) => {
        console.error('❌ Failed to load popular groups:', err);
        this.popularGroups = [];
      }
    });
  }

  /**
   * Navigate to discussion groups list
   */
  navigateToGroups() {
    this.router.navigate(['/groups']);
  }

  /**
   * Navigate to specific group
   */
  navigateToGroup(groupId: number) {
    this.router.navigate(['/groups', groupId]);
  }

  /**
   * Open group chat modal (Facebook-style)
   */
  openGroupChat(group: DiscussionGroup) {
    this.selectedGroupForChat = group;
    this.showGroupChatModal = true;
    this.loadGroupMessages(group.id!);
  }

  /**
   * Close group chat modal
   */
  closeGroupChat() {
    this.showGroupChatModal = false;
    this.selectedGroupForChat = null;
    this.groupMessages = [];
    this.groupMessageContent = '';
    this.showGroupEmojiPicker = false;
  }

  /**
   * Load messages for a group
   */
  loadGroupMessages(groupId: number) {
    this.groupMessageService.getGroupMessages(groupId).subscribe({
      next: (messages) => {
        this.groupMessages = messages;
        setTimeout(() => this.scrollGroupChatToBottom(), 100);
      },
      error: (err) => {
        console.error('❌ Failed to load group messages:', err);
        this.groupMessages = [];
      }
    });
  }

  /**
   * Send message in group chat
   */
  sendGroupMessage() {
    if (!this.groupMessageContent.trim() || this.sendingGroupMessage || !this.selectedGroupForChat) {
      return;
    }

    this.sendingGroupMessage = true;
    const message = {
      senderId: this.currentUserId!,
      senderName: this.currentUserName!,
      content: this.groupMessageContent,
      type: MessageType.TEXT
    };

    this.groupMessageService.sendMessage(this.selectedGroupForChat.id!, message).subscribe({
      next: (savedMessage) => {
        this.groupMessages.push(savedMessage);
        this.groupMessageContent = '';
        this.sendingGroupMessage = false;
        setTimeout(() => this.scrollGroupChatToBottom(), 100);
      },
      error: (err) => {
        console.error('❌ Failed to send message:', err);
        this.sendingGroupMessage = false;
      }
    });
  }

  /**
   * Check if message is from current user
   */
  isMyGroupMessage(message: any): boolean {
    return message.senderId === this.currentUserId;
  }

  /**
   * Toggle emoji picker in group chat
   */
  toggleGroupEmojiPicker() {
    this.showGroupEmojiPicker = !this.showGroupEmojiPicker;
  }

  /**
   * Add emoji to group message
   */
  addGroupEmoji(emoji: string) {
    this.groupMessageContent += emoji;
    this.showGroupEmojiPicker = false;
  }

  /**
   * Scroll group chat to bottom
   */
  scrollGroupChatToBottom() {
    const container = document.querySelector('.group-chat-messages');
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  }

  /**
   * Start voice recording with optimized settings
   */
  async startVoiceRecording() {
    try {
      // Request audio with optimized settings for faster processing
      const stream = await navigator.mediaDevices.getUserMedia({ 
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
          sampleRate: 16000 // Lower sample rate for smaller file size
        } 
      });
      
      // Use opus codec for better compression (smaller files, faster upload)
      const options = { 
        mimeType: 'audio/webm;codecs=opus',
        audioBitsPerSecond: 32000 // Lower bitrate for smaller files
      };
      
      // Fallback if opus not supported
      if (!MediaRecorder.isTypeSupported(options.mimeType)) {
        options.mimeType = 'audio/webm';
      }
      
      this.mediaRecorder = new MediaRecorder(stream, options);
      this.audioChunks = [];

      // Collect audio data in chunks for faster processing
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        const audioBlob = new Blob(this.audioChunks, { type: this.mediaRecorder!.mimeType });
        this.sendVoiceMessage(audioBlob);
        
        // Stop all tracks immediately
        stream.getTracks().forEach(track => track.stop());
      };

      // Request data every 100ms for smoother recording
      this.mediaRecorder.start(100);
      this.isRecording = true;
      this.recordingTime = 0;

      // Start timer - update every second
      this.recordingInterval = setInterval(() => {
        this.recordingTime++;
        
        // Max 2 minutes (120 seconds) for faster uploads
        if (this.recordingTime >= 120) {
          this.stopVoiceRecording();
          this.notificationService.warning('Max Duration', 'Maximum recording time reached (2 minutes)');
        }
      }, 1000);

      console.log('🎤 Voice recording started with optimized settings');
    } catch (error) {
      console.error('❌ Failed to start recording:', error);
      this.notificationService.error('Microphone Error', 'Could not access microphone. Please check permissions.');
    }
  }

  /**
   * Stop voice recording
   */
  stopVoiceRecording() {
    if (this.mediaRecorder && this.isRecording) {
      this.mediaRecorder.stop();
      this.isRecording = false;
      
      if (this.recordingInterval) {
        clearInterval(this.recordingInterval);
        this.recordingInterval = null;
      }
      
      console.log('🎤 Voice recording stopped, duration:', this.recordingTime, 'seconds');
    }
  }

  /**
   * Cancel voice recording
   */
  cancelVoiceRecording() {
    if (this.mediaRecorder && this.isRecording) {
      // Stop without triggering onstop callback
      this.isRecording = false;
      this.audioChunks = [];
      
      if (this.recordingInterval) {
        clearInterval(this.recordingInterval);
        this.recordingInterval = null;
      }
      
      if (this.mediaRecorder.state !== 'inactive') {
        // Remove onstop handler before stopping
        this.mediaRecorder.onstop = null;
        this.mediaRecorder.stop();
        
        // Stop all tracks
        if (this.mediaRecorder.stream) {
          this.mediaRecorder.stream.getTracks().forEach(track => track.stop());
        }
      }
      
      this.recordingTime = 0;
      console.log('🎤 Voice recording cancelled');
    }
  }

  /**
   * Send voice message with optimized base64 encoding
   */
  sendVoiceMessage(audioBlob: Blob) {
    if (!this.selectedGroupForChat) return;

    const duration = this.recordingTime;
    
    console.log('📤 Sending voice message, size:', (audioBlob.size / 1024).toFixed(2), 'KB');
    
    // Convert blob to base64 (optimized)
    const reader = new FileReader();
    reader.onloadend = () => {
      const base64Audio = reader.result as string;
      
      const message = {
        senderId: this.currentUserId!,
        senderName: this.currentUserName!,
        content: `🎤 Voice message (${this.formatDuration(duration)})`,
        type: MessageType.TEXT,
        audioData: base64Audio,
        audioDuration: duration
      };

      this.sendingGroupMessage = true;
      this.groupMessageService.sendMessage(this.selectedGroupForChat!.id!, message).subscribe({
        next: (savedMessage) => {
          // Add audio data to the saved message for playback
          savedMessage.audioData = base64Audio;
          savedMessage.audioDuration = duration;
          this.groupMessages.push(savedMessage);
          this.sendingGroupMessage = false;
          this.recordingTime = 0;
          setTimeout(() => this.scrollGroupChatToBottom(), 100);
          console.log('✅ Voice message sent successfully');
          this.notificationService.success('Sent', 'Voice message sent');
        },
        error: (err) => {
          console.error('❌ Failed to send voice message:', err);
          this.sendingGroupMessage = false;
          this.recordingTime = 0;
          this.notificationService.error('Error', 'Failed to send voice message');
        }
      });
    };
    
    // Use readAsDataURL for faster conversion
    reader.readAsDataURL(audioBlob);
  }

  /**
   * Play voice message
   */
  playVoiceMessage(audioData: string) {
    const audio = new Audio(audioData);
    audio.play().catch(err => {
      console.error('❌ Failed to play audio:', err);
      this.notificationService.error('Playback Error', 'Could not play voice message');
    });
  }

  /**
   * Format recording time (MM:SS)
   */
  formatRecordingTime(): string {
    const minutes = Math.floor(this.recordingTime / 60);
    const seconds = this.recordingTime % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  /**
   * Format duration from seconds (MM:SS)
   */
  formatDuration(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  }

  /**
   * Leave a group
   */
  leaveGroup(groupId: number, event: Event) {
    event.stopPropagation();
    
    if (!this.currentUserId) {
      console.warn('Cannot leave group: user not authenticated');
      return;
    }

    if (!confirm('Are you sure you want to leave this group?')) {
      return;
    }

    this.discussionGroupService.leaveGroup(groupId, this.currentUserId).subscribe({
      next: () => {
        // Remove from user groups
        this.userGroups = this.userGroups.filter(g => g.id !== groupId);
        
        // Close modal if this group is open
        if (this.selectedGroupForChat?.id === groupId) {
          this.closeGroupChat();
        }
        
        this.notificationService.success('Left group', 'You have left the group successfully');
        console.log('✅ Left group successfully');
      },
      error: (err) => {
        console.error('❌ Failed to leave group:', err);
        this.notificationService.error('Error', 'Failed to leave group');
      }
    });
  }

  /**
   * Report a message in group
   */
  reportGroupMessage(messageId: number, event: Event) {
    event.stopPropagation();
    
    if (!this.currentUserId || !this.selectedGroupForChat) {
      console.warn('Cannot report message: user not authenticated or no group selected');
      return;
    }

    const reason = prompt('Please provide a reason for reporting this message:');
    if (!reason || !reason.trim()) {
      return;
    }

    const report = {
      reporterId: this.currentUserId,
      reason: reason.trim(),
      description: ''
    };

    this.discussionGroupService.reportMessage(this.selectedGroupForChat.id!, messageId, report).subscribe({
      next: () => {
        this.notificationService.success('Report submitted', 'Thank you for helping keep our community safe');
        console.log('✅ Message reported successfully');
      },
      error: (err) => {
        console.error('❌ Failed to report message:', err);
        this.notificationService.error('Error', 'Failed to submit report');
      }
    });
  }

  /**
   * Navigate to create group page
   */
  createNewGroup() {
    this.router.navigate(['/groups/create']);
  }

  /**
   * Get default group logo
   */
  getDefaultGroupLogo(): string {
    return 'https://via.placeholder.com/50?text=G';
  }

  // ── Private Messages ──────────────────────────────────────────────────────
  
  /**
   * Load private chats
   */
  loadPrivateChats() {
    if (!this.currentUserId) {
      console.warn('Cannot load private chats: user not authenticated');
      return;
    }
    
    // TODO: Implement private messages service and load from backend
    // For now, initialize empty array (no static data)
    this.privateChats = [];
    this.unreadMessagesCount = 0;
    console.log('✅ Private chats initialized (awaiting backend implementation)');
  }

  /**
   * Open private chat
   */
  openPrivateChat(chatId: number) {
    // TODO: Implement private chat modal similar to group chat
    console.log('Opening private chat:', chatId);
  }

  // ── Friend Requests ───────────────────────────────────────────────────────
  
  /**
   * Load friend requests
   */
  loadFriendRequests() {
    if (!this.currentUserId) {
      console.warn('Cannot load friend requests: user not authenticated');
      return;
    }
    
    this.friendRequestService.getPendingRequestsReceived(this.currentUserId).subscribe({
      next: (requests) => {
        this.friendRequests = requests;
        this.friendRequestCount = requests.length;
        console.log('✅ Loaded friend requests:', requests.length);
      },
      error: (err) => {
        console.error('❌ Failed to load friend requests:', err);
        this.friendRequests = [];
        this.friendRequestCount = 0;
      }
    });
  }

  /**
   * Toggle friend requests panel
   */
  toggleFriendRequestsPanel() {
    this.showFriendRequestsPanel = !this.showFriendRequestsPanel;
    if (this.showFriendRequestsPanel) {
      this.showProfilePanel = false;
    }
  }

  /**
   * Accept friend request
   */
  acceptFriendRequest(requestId: number) {
    this.friendRequestService.acceptFriendRequest(requestId).subscribe({
      next: () => {
        this.friendRequests = this.friendRequests.filter(r => r.id !== requestId);
        this.friendRequestCount = this.friendRequests.length;
        this.notificationService.success('Friend request accepted', 'You are now friends!');
        console.log('✅ Friend request accepted');
      },
      error: (err) => {
        console.error('❌ Failed to accept friend request:', err);
        this.notificationService.error('Error', 'Failed to accept friend request');
      }
    });
  }

  /**
   * Reject friend request
   */
  rejectFriendRequest(requestId: number) {
    this.friendRequestService.rejectFriendRequest(requestId).subscribe({
      next: () => {
        this.friendRequests = this.friendRequests.filter(r => r.id !== requestId);
        this.friendRequestCount = this.friendRequests.length;
        this.notificationService.info('Friend request rejected', 'Request has been declined');
        console.log('✅ Friend request rejected');
      },
      error: (err) => {
        console.error('❌ Failed to reject friend request:', err);
        this.notificationService.error('Error', 'Failed to reject friend request');
      }
    });
  }

  // ── User Profile ──────────────────────────────────────────────────────────
  
  /**
   * Calculate user's post count
   */
  calculateUserPostCount() {
    // Count threads created by current user
    this.userPostCount = this.threads.filter(t => t.author === this.currentUserName).length;
    console.log('✅ User post count:', this.userPostCount);
  }

  /**
   * Toggle profile panel
   */
  toggleProfilePanel() {
    // Navigate to profile page instead of showing panel
    this.router.navigate(['/profile']);
  }

  // ── Forum Posts ───────────────────────────────────────────────────────────
  loadForumPosts() {
    this.forumApi.getTopLevelPosts().subscribe({
      next: (posts) => {
        this.forumPosts = posts;
        this.syncForumPostsToThreads(posts);
      },
      error: () => {
        // Forum backend may not be running; fall back to localStorage
      }
    });
  }

  private syncForumPostsToThreads(posts: ForumPost[]) {
    const forumThreads: Thread[] = posts.map(p => {
      // Determine category based on content or default to Freelancer hub
      let category = 'Freelancer hub';
      const content = (p.content ?? '').toLowerCase();
      
      // Smart categorization based on keywords
      if (content.includes('project') || content.includes('client') || content.includes('owner')) {
        category = 'Project owners';
      } else if (content.includes('collab') || content.includes('team') || content.includes('together')) {
        category = 'Open collab';
      } else if (content.includes('skill') || content.includes('learn') || content.includes('teach')) {
        category = 'Skill exchange';
      } else if (content.includes('job') || content.includes('hiring') || content.includes('opportunity') || content.includes('position')) {
        category = 'jobs';
      }
      
      return {
        id: undefined,
        forumPostId: p.id,
        title: (p.content ?? '').substring(0, 40),
        content: p.content ?? '',
        author: p.author ?? p.username ?? 'Unknown',
        authorRole: 'Member',
        authorAvatar: p.avatar ?? undefined,
        createdAt: p.createdAt ? new Date(p.createdAt as string) : new Date(),
        postCount: p.comments ?? 0,
        likes: p.likes ?? 0,
        dislikes: p.dislikes ?? 0,
        retweets: p.reposts ?? 0,
        tags: [],
        category: category,
        image: p.image ?? undefined,
        isEdited: p.isEdited ?? false,
        userId: p.userId ?? undefined,
        reposts: p.reposts ?? 0,
      };
    });

    // Merge: keep blog threads, prepend forum posts that aren't already present
    const existingForumIds = new Set(this.threads.filter(t => t.forumPostId).map(t => t.forumPostId));
    const newForumThreads = forumThreads.filter(t => !existingForumIds.has(t.forumPostId));
    this.threads = [...newForumThreads, ...this.threads];
    this.saveThreadsToStorage();
    this.updateTrendingTags();
    this.updateWhoToFollow();
  }

  loadForumTrendingTopics() {
    this.forumApi.getTrendingTopics().subscribe({
      next: (topics) => {
        if (topics && topics.length > 0) {
          this.trendingTags = topics.slice(0, 5).map(t => ({
            name: '#' + t.title.replace(/\s+/g, ''),
            postsCount: `${t.postCount ?? 0} posts`
          }));
        }
      },
      error: () => {}
    });
  }

  // ── Notifications ─────────────────────────────────────────────────────────
  loadNotifications() {
    if (!this.currentUserId) {
      console.warn('Cannot load notifications: user not authenticated');
      return;
    }
    
    this.forumApi.getUnreadNotificationCount(this.currentUserId).subscribe({
      next: (count) => { this.unreadNotificationCount = count; },
      error: () => {}
    });
  }

  toggleNotificationsPanel() {
    if (!this.currentUserId) {
      console.warn('Cannot toggle notifications: user not authenticated');
      return;
    }
    
    this.showNotificationsPanel = !this.showNotificationsPanel;
    if (this.showNotificationsPanel) {
      this.forumApi.getNotifications(this.currentUserId).subscribe({
        next: (notifs) => { this.notifications = notifs; },
        error: () => {}
      });
    }
  }

  markAllNotificationsRead() {
    if (!this.currentUserId) {
      console.warn('Cannot mark notifications read: user not authenticated');
      return;
    }
    
    this.forumApi.markAllNotificationsRead(this.currentUserId).subscribe({
      next: () => {
        this.notifications.forEach(n => n.isRead = true);
        this.unreadNotificationCount = 0;
      },
      error: () => {}
    });
  }

  // ── Liked / Reposted Forum Posts ──────────────────────────────────────────
  loadLikedForumPosts() {
    const stored = localStorage.getItem('likedForumPosts');
    this.likedForumPosts = stored ? JSON.parse(stored) : [];
  }

  saveLikedForumPosts() {
    localStorage.setItem('likedForumPosts', JSON.stringify(this.likedForumPosts));
  }

  loadDislikedForumPosts() {
    const stored = localStorage.getItem('dislikedForumPosts');
    this.dislikedForumPosts = stored ? JSON.parse(stored) : [];
  }

  saveDislikedForumPosts() {
    localStorage.setItem('dislikedForumPosts', JSON.stringify(this.dislikedForumPosts));
  }

  loadRepostedForumPosts() {
    const stored = localStorage.getItem('repostedForumPosts');
    this.repostedForumPosts = stored ? JSON.parse(stored) : [];
  }

  saveRepostedForumPosts() {
    localStorage.setItem('repostedForumPosts', JSON.stringify(this.repostedForumPosts));
  }

  isForumPostLiked(forumPostId: number): boolean {
    return this.likedForumPosts.includes(forumPostId);
  }

  isForumPostDisliked(forumPostId: number): boolean {
    return this.dislikedForumPosts.includes(forumPostId);
  }

  isForumPostReposted(forumPostId: number): boolean {
    return this.repostedForumPosts.includes(forumPostId);
  }

  toggleForumLike(thread: Thread, event: Event) {
    event.stopPropagation();
    if (!thread.forumPostId) return;
    const id = thread.forumPostId;
    const isLiked = this.isForumPostLiked(id);
    const call = isLiked ? this.forumApi.unlikePost(id) : this.forumApi.likePost(id);
    call.subscribe({
      next: (updated) => {
        thread.likes = updated.likes ?? 0;
        if (isLiked) {
          this.likedForumPosts = this.likedForumPosts.filter(x => x !== id);
        } else {
          // Remove dislike if exists
          if (this.isForumPostDisliked(id)) {
            this.dislikedForumPosts = this.dislikedForumPosts.filter(x => x !== id);
            this.saveDislikedForumPosts();
            thread.dislikes = Math.max(0, (thread.dislikes || 0) - 1);
          }
          this.likedForumPosts.push(id);
        }
        this.saveLikedForumPosts();
      }
    });
  }

  toggleForumDislike(thread: Thread, event: Event) {
    event.stopPropagation();
    if (!thread.forumPostId) return;
    const id = thread.forumPostId;
    const isDisliked = this.isForumPostDisliked(id);
    
    // Remove like if exists
    if (!isDisliked && this.isForumPostLiked(id)) {
      this.likedForumPosts = this.likedForumPosts.filter(x => x !== id);
      this.saveLikedForumPosts();
      thread.likes = Math.max(0, (thread.likes || 0) - 1);
    }
    
    // Toggle dislike
    if (isDisliked) {
      thread.dislikes = Math.max(0, (thread.dislikes || 0) - 1);
      this.dislikedForumPosts = this.dislikedForumPosts.filter(x => x !== id);
    } else {
      thread.dislikes = (thread.dislikes || 0) + 1;
      this.dislikedForumPosts.push(id);
    }
    this.saveDislikedForumPosts();
  }

  toggleForumRepost(thread: Thread, event: Event) {
    event.stopPropagation();
    if (!thread.forumPostId) return;
    const id = thread.forumPostId;
    if (this.isForumPostReposted(id)) return; // already reposted
    this.forumApi.repostPost(id).subscribe({
      next: (updated) => {
        thread.retweets = updated.reposts ?? 0;
        this.repostedForumPosts.push(id);
        this.saveRepostedForumPosts();
      }
    });
  }

  // ── Edit Forum Post ───────────────────────────────────────────────────────
  startEditPost(thread: Thread, event: Event) {
    event.stopPropagation();
    if (!thread.forumPostId) return;
    this.editingPostId = thread.forumPostId;
    this.editingContent = thread.content;
  }

  cancelEdit() {
    this.editingPostId = null;
    this.editingContent = '';
  }

  saveEdit(thread: Thread) {
    if (!thread.forumPostId || !this.editingContent.trim()) return;
    const updated: ForumPost = {
      content: this.editingContent.trim(),
      author: thread.author,
      username: thread.author,
      avatar: thread.authorAvatar,
    };
    this.forumApi.updatePost(thread.forumPostId, updated).subscribe({
      next: (saved) => {
        thread.content = saved.content ?? thread.content;
        thread.isEdited = true;
        this.cancelEdit();
        this.saveThreadsToStorage();
      },
      error: () => { this.cancelEdit(); }
    });
  }

  // ── Delete Forum Post ─────────────────────────────────────────────────────
  deleteForumPost(thread: Thread, event: Event) {
    event.stopPropagation();
    if (!thread.forumPostId) return;
    if (!confirm('Delete this post?')) return;
    this.forumApi.deletePost(thread.forumPostId).subscribe({
      next: () => {
        this.threads = this.threads.filter(t => t.forumPostId !== thread.forumPostId);
        this.saveThreadsToStorage();
      }
    });
  }

  // ── Report Forum Post ─────────────────────────────────────────────────────
  openReportModal(thread: Thread, event: Event) {
    event.stopPropagation();
    
    if (!this.currentUserId) {
      console.warn('Cannot report post: user not authenticated');
      return;
    }
    
    // Support both forum posts and blog threads
    if (thread.forumPostId) {
      this.reportingPostId = thread.forumPostId;
    } else if (thread.id) {
      this.reportingPostId = thread.id;
    } else {
      console.warn('Cannot report: no post ID found');
      return;
    }
    
    this.reportReason = '';
    this.reportDescription = '';
    console.log('✅ Report modal opened for post:', this.reportingPostId);
  }

  closeReportModal() {
    this.reportingPostId = null;
    this.reportReason = '';
    this.reportDescription = '';
  }

  submitReport() {
    if (!this.reportingPostId || !this.reportReason.trim() || !this.currentUserId) {
      console.warn('Cannot submit report: missing required fields');
      return;
    }
    
    const report: ForumReport = {
      postId: this.reportingPostId,
      reporterId: this.currentUserId,
      reason: this.reportReason,
      description: this.reportDescription,
    };
    
    console.log('📤 Submitting report:', report);
    
    this.forumApi.createReport(report).subscribe({
      next: () => { 
        this.closeReportModal(); 
        this.notificationService.success('Report Submitted', 'Thank you for helping keep our community safe.');
        console.log('✅ Report submitted successfully');
      },
      error: (err) => { 
        console.error('❌ Failed to submit report:', err);
        this.closeReportModal();
        this.notificationService.error('Error', 'Failed to submit report. Please try again.');
      }
    });
  }

  // ── Forum Replies ─────────────────────────────────────────────────────────
  /**
   * Toggle thread expansion (for blog threads)
   */
  toggleThreadExpansion(thread: Thread) {
    if (!thread.id) return;
    
    if (this.expandedThreadId === thread.id) {
      this.expandedThreadId = null;
    } else {
      this.expandedThreadId = thread.id;
      this.loadRepliesForThread(thread.id);
    }
  }

  /**
   * Toggle forum post expansion
   */
  toggleForumPostExpansion(thread: Thread) {
    if (!thread.forumPostId) return;
    const id = thread.forumPostId;
    if (this.expandedForumPostId === id) {
      this.expandedForumPostId = null;
    } else {
      this.expandedForumPostId = id;
      this.loadForumReplies(id);
    }
  }

  loadForumReplies(postId: number) {
    this.forumApi.getReplies(postId).subscribe({
      next: (replies) => { this.forumReplies[postId] = replies; },
      error: () => {}
    });
  }

  async submitForumReply(thread: Thread) {
    if (!thread.forumPostId || !this.forumInlineReplyContent.trim()) return;
    
    if (!this.currentUserId || !this.currentUserName) {
      console.warn('Cannot submit reply: user not authenticated');
      return;
    }
    
    const postId = thread.forumPostId;

    if (BANNED_WORDS_REGEX.test(this.forumInlineReplyContent)) {
      this.showToxicityPopup = true;
      return;
    }

    this.isCheckingContent = true;
    try {
      const isToxic = await this.toxicityApiService.checkToxicity(this.forumInlineReplyContent);
      if (isToxic) { this.showToxicityPopup = true; this.isCheckingContent = false; return; }

      const reply: ForumPost = {
        content: this.forumInlineReplyContent.trim(),
        author: this.currentUserName,
        username: this.currentUserName,
        userId: this.currentUserId,
        parentPostId: postId,
      };

      this.forumApi.createPost(reply).subscribe({
        next: (saved) => {
          if (!this.forumReplies[postId]) this.forumReplies[postId] = [];
          this.forumReplies[postId].push(saved);
          thread.postCount = (thread.postCount ?? 0) + 1;
          this.forumInlineReplyContent = '';
        }
      });
    } finally {
      this.isCheckingContent = false;
    }
  }

  // ── Image Upload ──────────────────────────────────────────────────────────
  onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    const reader = new FileReader();
    reader.onload = (e) => {
      this.newPostImage = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  }

  removeSelectedImage() {
    this.newPostImage = null;
  }

  // ── AI Text Correction ────────────────────────────────────────────────────
  correctPostContent() {
    console.log('🔥 FINAL VERSION - AI Correct button clicked!');
    console.log('📝 Current text:', this.newThreadBody);
    
    if (!this.newThreadBody.trim()) {
      this.notificationService.warning('Texte requis', 'Veuillez saisir du texte à corriger');
      return;
    }
    
    this.isCorrectingText = true;
    console.log('🚀 Using FINAL Gemini API call...');
    this.finalGeminiCall(this.newThreadBody);
  }

  private async finalGeminiCall(text: string) {
    console.log('🎯 === CALLING AI CORRECTION API ===');
    console.log('📊 Input length:', text.length);
    
    try {
      // Call backend AI correction service
      this.http.post<any>('http://localhost:8082/api/ai/correct-text', { text })
        .subscribe({
          next: (data) => {
            console.log('✅ AI correction response:', data);

            const correctedText = data.correctedText || text;
            const hasCorrections = data.hasCorrections || false;
            const explanation = data.explanation || '';
            const rateLimitError = data.rateLimitError || false;

            console.log('📝 Corrected text:', correctedText);
            console.log('📊 Has corrections:', hasCorrections);
            console.log('📊 Corrected text length:', correctedText.length);

            // Always update the text field
            this.newThreadBody = correctedText;

            // Show appropriate notification
            if (rateLimitError) {
              this.notificationService.error(
                '⏱️ Rate Limit Exceeded', 
                'Gemini API free tier: 60 requests/minute. Please wait 60 seconds and try again.'
              );
            } else if (hasCorrections && correctedText !== text) {
              this.notificationService.success(
                '✨ Text Corrected by AI', 
                explanation || 'All grammar and spelling errors have been fixed'
              );
            } else if (explanation.includes('unavailable') || explanation.includes('error')) {
              this.notificationService.warning(
                '⚠️ AI Temporarily Unavailable', 
                'Please try again in a few moments'
              );
            } else {
              this.notificationService.info(
                '✅ Perfect Text', 
                'No corrections needed - your text is already correct!'
              );
            }

            this.isCorrectingText = false;
          },
          error: (error) => {
            console.error('❌ AI correction failed:', error);
            
            // Check if it's a rate limit error from the HTTP response
            if (error.status === 429) {
              this.notificationService.error(
                '⏱️ Rate Limit Exceeded', 
                'Too many requests. Please wait 60 seconds and try again.'
              );
            } else {
              // Fallback to manual corrections
              const fallback = this.finalManualCorrections(text);
              this.newThreadBody = fallback;
              this.notificationService.warning(
                'Manual Correction', 
                'AI unavailable, basic corrections applied'
              );
            }
            
            this.isCorrectingText = false;
          }
        });

    } catch (error) {
      console.error('❌ Correction failed:', error);
      const fallback = this.finalManualCorrections(text);
      this.newThreadBody = fallback;
      this.notificationService.error('Correction Error', 'Basic corrections applied');
      this.isCorrectingText = false;
    }
  }

  private finalManualCorrections(text: string): string {
    console.log('🔧 Applying final manual corrections to:', text);
    
    const corrected = text
      // English corrections
      .replace(/\bI are\b/g, 'I am')
      .replace(/\bMy friend have\b/g, 'My friend has')
      .replace(/\bhave told\b/g, 'has told')
      .replace(/\bthey was\b/g, 'they were')
      .replace(/\bDo you wants\b/g, 'Do you want')
      .replace(/\byou wants\b/g, 'you want')
      .replace(/\bI go\b/g, 'I went')
      .replace(/\bI seen\b/g, 'I saw')
      .replace(/\bwas amazing\b/g, 'were amazing')
      .replace(/\bstory were\b/g, 'story was')
      .replace(/\bI will recommended\b/g, 'I would recommend')
      
      // French corrections - verb conjugation
      .replace(/\bje sommes\b/gi, 'je suis')
      .replace(/\bje allé\b/gi, 'je suis allé')
      .replace(/\bj'a acheté\b/gi, 'j\'ai acheté')
      .replace(/\bil avaient\b/gi, 'il avait')
      .replace(/\bnous mangerons\b/gi, 'nous avons mangé')
      .replace(/\bj'ai été content\b/gi, 'j\'étais content')
      .replace(/\bnous irons\b/gi, 'nous sommes allés')
      
      // French corrections - gender agreement
      .replace(/\bdes légume\b/gi, 'des légumes')
      .replace(/\btrès fraiches\b/gi, 'très frais')
      .replace(/\btrès fraiche\b/gi, 'très frais')
      
      // French corrections - common mistakes
      .replace(/\bdzl\b/gi, 'désolé')
      .replace(/\bpar la retard\b/gi, 'pour le retard')
      .replace(/\btres\b/gi, 'très')
      .replace(/\brencontré\b/g, 'rencontrer')
      .replace(/\bchose\b/g, 'choses')
      .replace(/\bdiscuté\b/g, 'discuter')
      .replace(/\bsa va\b/gi, 'ça va')
      
      // Punctuation fixes
      .replace(/\s+\?/g, '?')
      .replace(/\s+!/g, '!')
      .replace(/\s+\./g, '.')
      .replace(/\s+,/g, ',');
    
    console.log('✅ Manual corrections completed');
    console.log('📊 Manual - Original length:', text.length, 'Corrected length:', corrected.length);
    
    return corrected;
  }







  private applyManualCorrections(text: string): string {
    console.log('🔧 Starting manual corrections on text:', text);
    
    const corrected = text
      // English corrections
      .replace(/\bI are\b/g, 'I am')
      .replace(/\bMy friend have\b/g, 'My friend has')
      .replace(/\bhave told\b/g, 'has told')
      .replace(/\bthey was\b/g, 'they were')
      .replace(/\bDo you wants\b/g, 'Do you want')
      .replace(/\byou wants\b/g, 'you want')
      .replace(/\bI go\b/g, 'I went')
      .replace(/\bI seen\b/g, 'I saw')
      .replace(/\bwas amazing\b/g, 'were amazing')
      .replace(/\bstory were\b/g, 'story was')
      .replace(/\bI will recommended\b/g, 'I would recommend')
      .replace(/\bI have (\d+) years old\b/g, 'I am $1 years old')
      .replace(/\bsince (\d+) years\b/g, 'for $1 years')
      .replace(/\bwork like\b/g, 'work as')
      .replace(/\bpassioned by\b/g, 'passionate about')
      
      // French corrections
      .replace(/\bje sommes\b/gi, 'je suis')
      .replace(/\bdzl\b/gi, 'désolé')
      .replace(/\bpar la retard\b/gi, 'pour le retard')
      .replace(/\bpar le retard\b/gi, 'pour le retard')
      .replace(/\btres\b/gi, 'très')
      .replace(/\brencontré\b/g, 'rencontrer')
      .replace(/\bchose\b/g, 'choses')
      .replace(/\bdiscuté\b/g, 'discuter')
      .replace(/\bsa va\b/gi, 'ça va')
      .replace(/\bpassé\b/g, 'passer')
      .replace(/\bdesole\b/gi, 'désolé')
      .replace(/\bprobleme\b/gi, 'problème')
      .replace(/\benvoye\b/gi, 'envoyer')
      .replace(/\breporte\b/gi, 'reporter')
      .replace(/\breunion\b/gi, 'réunion')
      .replace(/\bdifficulte\b/gi, 'difficulté')
      .replace(/\betude\b/gi, 'études')
      .replace(/\bexigent\b/gi, 'exigeants')
      .replace(/\btravail\b/g, 'travailler')
      .replace(/\bm'aide\b/gi, 'm\'aider')
      
      // Punctuation fixes
      .replace(/\s+\?/g, '?')
      .replace(/\s+!/g, '!')
      .replace(/\s+\./g, '.')
      .replace(/\s+,/g, ',')
      
      // Capitalization fixes
      .replace(/^([a-z])/gm, (match) => match.toUpperCase())
      .replace(/\.\s+([a-z])/g, (match, letter) => '. ' + letter.toUpperCase());
    
    console.log('🎯 Manual corrections completed. Changes made:', text !== corrected);
    console.log('📊 Original vs Corrected lengths:', text.length, '→', corrected.length);
    
    return corrected;
  }

  loadThreads() {
    // Note: localStorage threads are now cleared on init for microservices architecture
    // All threads should come from the backend (forum posts)
    const storedThreads = localStorage.getItem('threads');
    if (storedThreads) {
      this.threads = JSON.parse(storedThreads).map((thread: any) => {
        const createdAt = thread.createdAt ? new Date(thread.createdAt) : new Date();
        return {
          id: thread.id,
          title: thread.title ?? (typeof thread.content === 'string' ? thread.content.substring(0, 40) : 'Thread'),
          content: thread.content ?? '',
          author: thread.author ?? 'Current User',
          authorRole: thread.authorRole ?? 'Member',
          authorAvatar: thread.authorAvatar,
          createdAt,
          postCount: thread.postCount ?? thread.replies ?? 0,
          likes: thread.likes ?? 0,
          retweets: thread.retweets ?? 0,
          tags: Array.isArray(thread.tags) ? thread.tags : [],
          category: thread.category ?? 'Freelancer hub',
        } as Thread;
      });
    } else {
      // No static posts - all posts come from backend
      this.threads = [];
    }
    this.updateTrendingTags();
    this.updateWhoToFollow();
  }

  private saveThreadsToStorage() {
    localStorage.setItem('threads', JSON.stringify(this.threads));
  }

  loadLikedThreads() {
    const stored = localStorage.getItem('likedThreads');
    this.likedThreads = stored ? JSON.parse(stored) : [];
  }

  saveLikedThreads() {
    localStorage.setItem('likedThreads', JSON.stringify(this.likedThreads));
  }

  loadDislikedThreads() {
    const stored = localStorage.getItem('dislikedThreads');
    this.dislikedThreads = stored ? JSON.parse(stored) : [];
  }

  saveDislikedThreads() {
    localStorage.setItem('dislikedThreads', JSON.stringify(this.dislikedThreads));
  }

  loadRetweetedThreads() {
    const stored = localStorage.getItem('retweetedThreads');
    this.retweetedThreads = stored ? JSON.parse(stored) : [];
  }

  saveRetweetedThreads() {
    localStorage.setItem('retweetedThreads', JSON.stringify(this.retweetedThreads));
  }

  loadFollowedAuthors() {
    const stored = localStorage.getItem('followedAuthors');
    this.followedAuthors = stored ? JSON.parse(stored) : [];
  }

  saveFollowedAuthors() {
    localStorage.setItem('followedAuthors', JSON.stringify(this.followedAuthors));
  }

  updateTrendingTags() {
    const tagCounts: { [key: string]: number } = {};
    this.threads.forEach(t => {
      t.tags.forEach(tag => {
        tagCounts[tag] = (tagCounts[tag] || 0) + 1;
      });
    });
    this.trendingTags = Object.keys(tagCounts)
      .sort((a, b) => tagCounts[b] - tagCounts[a])
      .slice(0, 5)
      .map(tag => ({ name: tag, postsCount: `${tagCounts[tag]} post${tagCounts[tag] > 1 ? 's' : ''} here` }));
  }

  updateWhoToFollow() {
    const uniqueAuthors = new Map<string, Thread>();
    this.threads.forEach(t => {
      if (!uniqueAuthors.has(t.author)) {
        uniqueAuthors.set(t.author, t);
      }
    });

    this.whoToFollow = Array.from(uniqueAuthors.values())
      .filter(t => t.author !== this.currentUserName) // Don't suggest following self
      .map(t => ({
        name: t.author,
        role: t.authorRole,
        avatar: t.authorAvatar || '',
        initials: this.getInitials(t.author),
        isFollowing: this.followedAuthors.includes(t.author)
      }))
      .slice(0, 3);
  }

  getFilteredThreads() {
    let filtered = [...this.threads];
    
    // Sidebar Navigation Filtering
    if (this.activeNav === 'following') {
      filtered = filtered.filter(t => this.followedAuthors.includes(t.author));
    } else if (this.activeNav === 'trending') {
      // Sort by engagement score (likes - dislikes + retweets + comments) - highest first
      filtered = filtered.sort((a, b) => {
        const engagementA = (a.likes || 0) - (a.dislikes || 0) + (a.retweets || 0) + (a.postCount || 0);
        const engagementB = (b.likes || 0) - (b.dislikes || 0) + (b.retweets || 0) + (b.postCount || 0);
        return engagementB - engagementA;
      });
    } else if (this.activeNav === 'for-you') {
      // Filter by user's role - show relevant content
      if (this.currentUserRole) {
        filtered = filtered.filter(t => {
          // Show posts from same role OR posts in relevant categories
          const sameRole = t.authorRole === this.currentUserRole;
          
          // Freelancers see: Freelancer hub, Skill exchange, Jobs
          // Project Owners see: Project owners, Open collab, Jobs
          let relevantCategory = false;
          if (this.currentUserRole === 'Freelancer') {
            relevantCategory = ['Freelancer hub', 'Skill exchange', 'jobs'].includes(t.category);
          } else if (this.currentUserRole === 'Project Owner' || this.currentUserRole === 'PROJECT_OWNER') {
            relevantCategory = ['Project owners', 'Open collab', 'jobs'].includes(t.category);
          }
          
          return sameRole || relevantCategory;
        });
      }
      
      // Sort by recency for "For You"
      filtered = filtered.sort((a, b) => {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      });
    } else if (this.categories.some(c => c.value === this.activeNav)) {
      filtered = filtered.filter(t => t.category === this.activeNav);
    }
    
    // Secondary Category Selection (Tabs or Select)
    if (this.selectedCategory !== 'all' && this.activeNav === 'for-you') {
      filtered = filtered.filter(thread => thread.category === this.selectedCategory);
    }
    
    // Tag Filtering
    if (this.selectedTag) {
      filtered = filtered.filter(thread => thread.tags.includes(this.selectedTag!));
    }
    
    return filtered;
  }

  getCategoryLabel(categoryValue: string) {
    const category = this.categories.find(cat => cat.value === categoryValue);
    return category ? category.label : categoryValue;
  }

  getCategoryColor(category: string) {
    const colors: { [key: string]: string } = {
      general: 'bg-gray-100 text-gray-800',
      projects: 'bg-blue-100 text-blue-800',
      help: 'bg-green-100 text-green-800',
      showcase: 'bg-purple-100 text-purple-800',
      jobs: 'bg-orange-100 text-orange-800'
    };
    return colors[category] || 'bg-gray-100 text-gray-800';
  }

  formatTimeAgo(date: Date) {
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInHours / 24);

    if (diffInDays > 0) {
      return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`;
    } else if (diffInHours > 0) {
      return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  }

  truncateContent(content: string, maxLength: number = 200) {
    if (content.length <= maxLength) {
      return content;
    }
    return content.substring(0, maxLength) + '...';
  }

  viewThread(thread: Thread) {
    if (thread.id === undefined) return;
    this.toggleThreadExpansion(thread);
  }

  openDrawer() {
    this.isDrawerOpen = true;
    
    // Set default category based on user role
    if (this.currentUserRole === 'Freelancer') {
      this.newThreadCategory = 'Freelancer hub';
    } else if (this.currentUserRole === 'Project Owner' || this.currentUserRole === 'PROJECT_OWNER') {
      this.newThreadCategory = 'Project owners';
    } else {
      this.newThreadCategory = 'Freelancer hub';
    }
  }

  closeDrawer() {
    this.isDrawerOpen = false;
    this.newThreadBody = '';
    this.newThreadTags = [];
    this.tagInput = '';
    this.newThreadCategory = 'Freelancer hub';
  }

  toggleSuggestedTag(tag: string) {
    if (this.newThreadTags.includes(tag)) {
      this.newThreadTags = this.newThreadTags.filter(t => t !== tag);
    } else {
      this.newThreadTags.push(tag);
    }
  }

  addTag(event: KeyboardEvent) {
    if ((event.key === 'Enter' || event.key === ',') && this.tagInput.trim()) {
      event.preventDefault();
      const tag = this.tagInput.trim().startsWith('#')
        ? this.tagInput.trim()
        : '#' + this.tagInput.trim();
      if (!this.newThreadTags.includes(tag)) {
        this.newThreadTags.push(tag);
      }
      this.tagInput = '';
    }
  }

  removeTag(tag: string) {
    this.newThreadTags = this.newThreadTags.filter(t => t !== tag);
  }

  async postThread() {
    if (!this.newThreadBody.trim()) return;

    if (!this.currentUserId || !this.currentUserName || !this.currentUserRole) {
      console.warn('Cannot post thread: user not authenticated');
      return;
    }

    // --- SYNC CHECK (Fastest) ---
    if (BANNED_WORDS_REGEX.test(this.newThreadBody)) {
      this.showToxicityPopup = true;
      return;
    }

    this.isCheckingContent = true;
    try {
      const isToxic = await this.toxicityApiService.checkToxicity(this.newThreadBody);
      if (isToxic) {
        this.showToxicityPopup = true;
        this.isCheckingContent = false;
        return;
      }

      const newThread: any = {
        title: this.newThreadBody.trim().substring(0, 40) + (this.newThreadBody.length > 40 ? '...' : ''),
        content: this.newThreadBody.trim(),
        author: this.currentUserName,
        authorId: this.currentUserId,  // Required by backend
        authorRole: this.currentUserRole,
        tags: this.newThreadTags,
        category: this.newThreadCategory,
        likes: 0,
        retweets: 0
      };

      // Also create in forum backend
      const forumPost: ForumPost = {
        content: this.newThreadBody.trim(),
        author: this.currentUserName,
        username: this.currentUserName,
        userId: this.currentUserId,
        image: this.newPostImage ?? undefined,
        likes: 0,
        reposts: 0,
        comments: 0,
      };

      this.threadApi.createThread(newThread).subscribe({
        next: (saved) => {
          const thread: Thread = {
            id: saved.id,
            title: saved.title || '',
            content: saved.content || '',
            author: saved.author || this.currentUserName || 'Unknown',
            authorRole: this.currentUserRole || 'Member',
            authorAvatar: saved.authorAvatar,
            createdAt: new Date(saved.createdAt!),
            postCount: 0,
            likes: 0,
            dislikes: 0,
            retweets: 0,
            tags: this.newThreadTags,
            category: this.newThreadCategory
          };
          // Try to also persist in forum backend
          this.forumApi.createPost(forumPost).subscribe({
            next: (fp) => { thread.forumPostId = fp.id; thread.image = fp.image; },
            error: () => {}
          });
          this.threads.unshift(thread);
          this.saveThreadsToStorage();
          this.updateTrendingTags();
          this.updateWhoToFollow();
          this.newPostImage = null;
          this.closeDrawer();
        },
        error: (err) => {
          console.error('Failed to post thread. Full Server Response:', err);
          const serverMessage = err.error?.message || 'Unknown Server Error';
          if (err.error?.error === 'MODERATION_ERROR') {
            this.showToxicityPopup = true;
          } else {
            alert('Server Error: ' + serverMessage);
          }
        }
      });
    } finally {
      this.isCheckingContent = false;
    }
  }

  getInitials(name: string | null): string {
    if (!name) return 'U'; // Return 'U' for unknown/unauthenticated user
    
    const parts = name.trim().split(/\s+/).filter(Boolean);
    const first = parts[0]?.[0] ?? 'U';
    const last = (parts.length > 1 ? parts[parts.length - 1]?.[0] : '') ?? '';
    return (first + last).toUpperCase();
  }

  formatTime(dateInput: Date | string | undefined): string {
    if (!dateInput) return '...';
    const date = dateInput instanceof Date ? dateInput : new Date(dateInput);
    const diff = Math.floor((new Date().getTime() - date.getTime()) / 60000);
    if (diff < 1) return 'now';
    if (diff < 60) return `${diff}m`;
    if (diff < 1440) return `${Math.floor(diff / 60)}h`;
    return `${Math.floor(diff / 1440)}d`;
  }

  filterByTag(tagName: string) {
    if (this.selectedTag === tagName) {
      this.selectedTag = null; // Toggle off if already selected
    } else {
      this.selectedTag = tagName;
    }
  }

  toggleFollow(user: WhoToFollow) {
    if (this.followedAuthors.includes(user.name)) {
      this.followedAuthors = this.followedAuthors.filter(a => a !== user.name);
      user.isFollowing = false;
    } else {
      this.followedAuthors.push(user.name);
      user.isFollowing = true;
    }
    this.saveFollowedAuthors();
  }

  selectNav(item: string) {
    this.activeNav = item;
    this.selectedTag = null;
    if (this.categories.some(c => c.value === item)) {
      this.newPostCategory = item;
    }
  }

  toggleLike(thread: Thread, event: Event) {
    if (thread.id === undefined) return;
    event.stopPropagation();
    const isLiked = this.isLiked(thread.id);
    
    this.threadApi.toggleLike(thread.id!, !isLiked).subscribe({
      next: (updated) => {
        thread.likes = updated.likes;
        const index = this.likedThreads.indexOf(thread.id!);
        if (index === -1) {
          this.likedThreads.push(thread.id!);
        } else {
          this.likedThreads.splice(index, 1);
        }
        this.saveLikedThreads();
      }
    });
  }

  isLiked(id: number | undefined): boolean {
    if (id === undefined) return false;
    return this.likedThreads.includes(id);
  }

  toggleDislike(thread: Thread, event: Event) {
    if (thread.id === undefined) return;
    event.stopPropagation();
    const isDisliked = this.isDisliked(thread.id);
    
    // If disliking, remove like first
    if (!isDisliked && this.isLiked(thread.id)) {
      this.toggleLike(thread, event);
    }
    
    // Toggle dislike
    if (isDisliked) {
      thread.dislikes = Math.max(0, (thread.dislikes || 0) - 1);
      const index = this.dislikedThreads.indexOf(thread.id!);
      if (index !== -1) {
        this.dislikedThreads.splice(index, 1);
      }
    } else {
      thread.dislikes = (thread.dislikes || 0) + 1;
      this.dislikedThreads.push(thread.id!);
    }
    this.saveDislikedThreads();
  }

  isDisliked(id: number | undefined): boolean {
    if (id === undefined) return false;
    return this.dislikedThreads.includes(id);
  }

  toggleRetweet(thread: Thread, event: Event) {
    if (thread.id === undefined) return;
    event.stopPropagation();
    const isRetweeted = this.isRetweeted(thread.id);
    
    this.threadApi.toggleRetweet(thread.id!, !isRetweeted).subscribe({
      next: (updated) => {
        thread.retweets = updated.retweets;
        const index = this.retweetedThreads.indexOf(thread.id!);
        if (index === -1) {
          this.retweetedThreads.push(thread.id!);
        } else {
          this.retweetedThreads.splice(index, 1);
        }
        this.saveRetweetedThreads();
      }
    });
  }

  isRetweeted(id: number | undefined): boolean {
    if (id === undefined) return false;
    return this.retweetedThreads.includes(id);
  }

  openReply(thread: Thread, event: Event) {
    if (thread.id === undefined) return;
    event.stopPropagation();
    if (this.expandedThreadId === thread.id) {
      this.expandedThreadId = null;
    } else {
      this.expandedThreadId = thread.id;
      this.loadRepliesForThread(thread.id);
    }
  }

  loadRepliesForThread(threadId: number | undefined) {
    if (threadId === undefined) return;
    this.threadApi.getComments(threadId).subscribe({
      next: (data) => {
        this.threadReplies[threadId] = data.map(r => ({
          ...r,
          createdAt: new Date(r.createdAt!)
        }));
      }
    });
  }

  async submitInlineReply(thread: Thread) {
    if (thread.id === undefined || !this.inlineReplyContent.trim()) return;
    
    if (!this.currentUserName || !this.currentUserRole) {
      console.warn('Cannot submit reply: user not authenticated');
      return;
    }
    
    const threadId = thread.id;

    // --- SYNC CHECK (Fastest) ---
    if (BANNED_WORDS_REGEX.test(this.inlineReplyContent)) {
      this.showToxicityPopup = true;
      return;
    }

    this.isCheckingContent = true;
    try {
      const isToxic = await this.toxicityApiService.checkToxicity(this.inlineReplyContent);
      if (isToxic) {
        this.showToxicityPopup = true;
        this.isCheckingContent = false;
        return;
      }

      const newReply: any = {
        author: this.currentUserName,
        authorRole: this.currentUserRole,
        content: this.inlineReplyContent,
        likes: 0
      };

      this.threadApi.addComment(threadId, newReply).subscribe({
        next: (saved) => {
          if (!this.threadReplies[threadId]) {
            this.threadReplies[threadId] = [];
          }
          this.threadReplies[threadId].push({
            ...saved,
            createdAt: new Date(saved.createdAt!)
          });
          thread.postCount = (thread.postCount || 0) + 1;
          this.inlineReplyContent = '';
        },
        error: (err) => {
          console.error('Failed to add reply', err);
          if (err.error?.error === 'MODERATION_ERROR') {
            this.showToxicityPopup = true;
          }
        }
      });
    } finally {
      this.isCheckingContent = false;
    }
  }

  // Unique tracking function for Angular @for directive
  trackThread(index: number, thread: Thread): string {
    // Create a unique identifier that combines both possible ID sources
    if (thread.forumPostId) {
      return `forum-${thread.forumPostId}`;
    } else if (thread.id) {
      return `thread-${thread.id}`;
    } else {
      // Fallback to index + timestamp + content hash for threads without IDs
      const contentHash = thread.content.substring(0, 20).replace(/\s/g, '');
      const timestamp = thread.createdAt ? new Date(thread.createdAt).getTime() : Date.now();
      return `temp-${index}-${timestamp}-${contentHash}`;
    }
  }
}
