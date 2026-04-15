import { Component, OnInit, ViewChild, ElementRef, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UserService } from '../../../services/user.service';
import { ToastService } from '../../../services/toast.service';
import { User } from '../../../models/user.model';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-avatar-upload',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="relative group cursor-pointer flex items-center justify-center h-10 w-10 rounded-full bg-indigo-100 border-2 border-transparent hover:border-indigo-400 transition" 
         (click)="mode === 'nav' ? goToProfile() : fileInput.click()">
      @if (loading) {
        <div class="absolute inset-0 flex items-center justify-center bg-white/70 rounded-full">
           <div class="animate-spin h-5 w-5 border-2 border-indigo-600 rounded-full border-t-transparent"></div>
        </div>
      }
      
      @if (fullAvatarUrl) {
         <img [src]="fullAvatarUrl" alt="Avatar" class="h-full w-full rounded-full object-cover">
      } @else {
         <span class="text-sm font-bold text-indigo-700">{{ userInitials | uppercase }}</span>
      }
      
      <div class="absolute -bottom-1 -right-1 bg-white rounded-full p-0.5 shadow border border-gray-200 opacity-0 group-hover:opacity-100 transition z-10">
        <svg class="w-3 h-3 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </div>

      <input #fileInput type="file" class="hidden" accept="image/*" (change)="onFileSelected($event)">
    </div>
  `
})
export class AvatarUploadComponent implements OnInit {
    @Input() mode: 'nav' | 'upload' = 'nav';
    user: User | null = null;
    loading = false;

    @Output() avatarUpdated = new EventEmitter<string>();
    @ViewChild('fileInput') fileInput!: ElementRef;

    constructor(
        private userService: UserService,
        private toastService: ToastService,
        private router: Router
    ) { }

    ngOnInit() {
        this.fetchUser();
    }

    goToProfile() {
        this.router.navigate(['/profile']);
    }

    fetchUser() {
        this.userService.getCurrentUser().subscribe({
            next: (u) => {
                this.user = u;
            },
            error: () => { }
        });
    }

    get userInitials(): string {
        if (!this.user) return '?';
        return (this.user.firstName?.charAt(0) || '') + (this.user.lastName?.charAt(0) || '');
    }

    get fullAvatarUrl(): string | null {
        if (!this.user || !this.user.profilePictureUrl) return null;
        const serverBaseUrl = environment.apiUrl.replace(/\/api\/?$/, '');
        return serverBaseUrl + this.user.profilePictureUrl;
    }

    onFileSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files.length > 0) {
            const file = input.files[0];

            // Basic validation
            if (!file.type.startsWith('image/')) {
                this.toastService.error('Please select an image file');
                return;
            }
            if (file.size > 5 * 1024 * 1024) {
                this.toastService.error('Image size must be less than 5MB');
                return;
            }

            this.loading = true;
            this.userService.uploadAvatar(file).subscribe({
                next: (res) => {
                    if (this.user) {
                        this.user.profilePictureUrl = res.url;
                    }
                    this.avatarUpdated.emit(res.url);
                    this.loading = false;
                    this.toastService.success('Avatar updated successfully!');
                },
                error: (err) => {
                    this.loading = false;
                    this.toastService.error('Failed to upload avatar. ' + (err.error || ''));
                }
            });
            // reset file input
            input.value = '';
        }
    }
}
