import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { AvatarUploadComponent } from '../shared/avatar-upload/avatar-upload.component';

@Component({
  selector: 'app-client-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, AvatarUploadComponent],
  templateUrl: './client-layout.component.html',
})
export class ClientLayoutComponent {
  constructor(private authService: AuthService) { }

  logout(): void {
    this.authService.logout();
  }

  goToPasswordChange() {
    const email = this.authService.getUserEmail();
    if (email) {
      // Navigate to password change page with email pre-filled
      window.location.href = `/change-password?email=${encodeURIComponent(email)}`;
    } else {
      window.location.href = '/change-password';
    }
  }
}
