import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AvatarUploadComponent } from '../shared/avatar-upload/avatar-upload.component';

@Component({
  selector: 'app-front-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, AvatarUploadComponent],
  templateUrl: './front-layout.component.html',
})
export class FrontLayoutComponent {
  constructor(private authService: AuthService, private router: Router) { }

  handleClientClick(): void {
    if (this.authService.isLoggedIn()) {
      const userRole = this.authService.getUserRole();
      if (userRole === 'ADMIN') {
        this.router.navigate(['/admin/projects']);
      } else {
        this.router.navigate(['/projects']);
      }
    } else {
      this.router.navigate(['/login']);
    }
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

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
