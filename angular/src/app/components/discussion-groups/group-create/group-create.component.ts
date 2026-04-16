import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DiscussionGroupService } from '../../../services/discussion-group.service';

@Component({
  selector: 'app-group-create',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './group-create.component.html',
  styleUrls: ['./group-create.component.css']
})
export class GroupCreateComponent {
  group = {
    name: '',
    topic: 'Freelancing',
    logoUrl: '',
    isPrivate: false,
    allowMemberInvites: true,
    allowGifs: true,
    allowEmojis: true,
    creatorId: 1, // Mock user ID
    creatorName: 'Test User' // Mock user name
  };

  topics: string[] = ['Freelancing', 'Technology', 'Design', 'Business', 'Marketing', 'Other'];
  creating: boolean = false;
  uploading: boolean = false;
  error: string = '';
  
  // Drag & Drop
  isDragging: boolean = false;
  logoPreview: string | null = null;
  logoFile: File | null = null;

  constructor(
    private groupService: DiscussionGroupService,
    private router: Router
  ) {}

  // Drag & Drop handlers
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.handleFile(files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  handleFile(file: File): void {
    // Validate file type
    if (!file.type.startsWith('image/')) {
      this.error = 'Please select an image file';
      return;
    }

    // Validate file size (5MB)
    if (file.size > 5 * 1024 * 1024) {
      this.error = 'Image size must be less than 5MB';
      return;
    }

    this.logoFile = file;
    this.error = '';

    // Create preview
    const reader = new FileReader();
    reader.onload = (e) => {
      this.logoPreview = e.target?.result as string;
      this.group.logoUrl = this.logoPreview; // Set as base64 for now
    };
    reader.readAsDataURL(file);
  }

  removeLogo(event: Event): void {
    event.stopPropagation();
    this.logoFile = null;
    this.logoPreview = null;
    this.group.logoUrl = '';
  }

  createGroup(): void {
    if (!this.validateForm()) {
      return;
    }

    this.creating = true;
    this.error = '';

    // If logo file exists, we could upload it first
    // For now, we'll use the base64 preview as logoUrl
    this.groupService.createGroup(this.group).subscribe({
      next: (createdGroup) => {
        console.log('✅ Group created:', createdGroup);
        // Navigate back to threads page
        this.router.navigate(['/threads']);
      },
      error: (error) => {
        console.error('❌ Error creating group:', error);
        this.error = 'Failed to create group. Please try again.';
        this.creating = false;
      }
    });
  }

  validateForm(): boolean {
    if (!this.group.name.trim()) {
      this.error = 'Group name is required';
      return false;
    }

    if (this.group.name.length < 3) {
      this.error = 'Group name must be at least 3 characters';
      return false;
    }

    return true;
  }

  cancel(): void {
    this.router.navigate(['/threads']);
  }
}
