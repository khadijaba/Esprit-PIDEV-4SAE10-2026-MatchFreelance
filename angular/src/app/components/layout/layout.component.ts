import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './layout.component.html',
})
export class LayoutComponent {
  sidebarOpen = true;

  constructor(private authService: AuthService) {}

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
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

