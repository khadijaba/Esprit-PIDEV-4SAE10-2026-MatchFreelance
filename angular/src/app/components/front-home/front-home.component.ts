import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

interface Thread {
  id: number;
  title: string;
  content: string;
  author: string;
  createdAt: Date;
  replies: number;
  category: string;
}

@Component({
  selector: 'app-front-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './front-home.component.html',
})
export class FrontHomeComponent implements OnInit {
  threadTitle = '';
  threadContent = '';
  threadCategory = 'general';
  
  categories = [
    { value: 'general', label: 'General Discussion' },
    { value: 'projects', label: 'Projects' },
    { value: 'help', label: 'Help & Support' },
    { value: 'showcase', label: 'Showcase' },
    { value: 'jobs', label: 'Jobs & Opportunities' }
  ];

  constructor(private router: Router) {}

  ngOnInit() {}

  createThread() {
    if (this.threadTitle.trim() && this.threadContent.trim()) {
      // TODO: Replace with User microservice integration
      // For now, check if user is authenticated
      const currentUser = this.getCurrentUser();
      if (!currentUser) {
        console.warn('Cannot create thread: user not authenticated');
        alert('Please log in to create a thread');
        return;
      }
      
      // Get existing threads from localStorage or initialize empty array
      const existingThreads = JSON.parse(localStorage.getItem('threads') || '[]');
      
      const newThread: Thread = {
        id: Date.now(),
        title: this.threadTitle,
        content: this.threadContent,
        author: currentUser.name,
        createdAt: new Date(),
        replies: 0,
        category: this.threadCategory
      };
      
      // Add new thread to the beginning of the array
      existingThreads.unshift(newThread);
      
      // Save back to localStorage
      localStorage.setItem('threads', JSON.stringify(existingThreads));
      
      // Navigate to threads page
      this.router.navigate(['/threads']);
      
      // Reset form
      this.threadTitle = '';
      this.threadContent = '';
      this.threadCategory = 'general';
    }
  }

  /**
   * Get current user data from User microservice
   * TODO: Replace with actual authentication service
   */
  private getCurrentUser(): { id: number; name: string; role: string } | null {
    // TEMPORARY: Mock user data for testing (remove when User microservice is integrated)
    return {
      id: 1,
      name: 'Test User',
      role: 'Freelancer'
    };
    
    // TODO: Implement actual user authentication check
    // Example implementation:
    // return this.authService.getCurrentUser();
    
    // console.log('User authentication check - waiting for User microservice integration');
    // return null;
  }
}
