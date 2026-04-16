import { Injectable } from '@angular/core';

/**
 * Service to manage localStorage cleanup for microservices architecture
 * Removes static/test data to prepare for User microservice integration
 */
@Injectable({
  providedIn: 'root'
})
export class StorageCleanupService {

  constructor() { }

  /**
   * Clear all static posts from localStorage
   * This should be called once during app initialization
   */
  clearStaticPosts(): void {
    const keysToRemove = ['threads'];
    
    keysToRemove.forEach(key => {
      if (localStorage.getItem(key)) {
        localStorage.removeItem(key);
        console.log(`🗑️ Cleared static data: ${key}`);
      }
    });
    
    console.log('✅ Static posts cleanup complete');
  }

  /**
   * Clear all blog-related localStorage data
   * Use this for complete cleanup
   */
  clearAllBlogData(): void {
    const blogKeys = [
      'threads',
      'likedThreads',
      'retweetedThreads',
      'followedAuthors',
      'likedForumPosts',
      'repostedForumPosts'
    ];
    
    blogKeys.forEach(key => {
      if (localStorage.getItem(key)) {
        localStorage.removeItem(key);
        console.log(`🗑️ Cleared: ${key}`);
      }
    });
    
    console.log('✅ All blog data cleared from localStorage');
  }

  /**
   * Check if static posts exist in localStorage
   */
  hasStaticPosts(): boolean {
    return localStorage.getItem('threads') !== null;
  }

  /**
   * Get count of static posts in localStorage
   */
  getStaticPostsCount(): number {
    const threads = localStorage.getItem('threads');
    if (!threads) return 0;
    
    try {
      const parsed = JSON.parse(threads);
      return Array.isArray(parsed) ? parsed.length : 0;
    } catch {
      return 0;
    }
  }
}