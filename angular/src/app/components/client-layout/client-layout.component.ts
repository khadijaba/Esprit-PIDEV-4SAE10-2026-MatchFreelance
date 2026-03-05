import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-client-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './client-layout.component.html',
})
export class ClientLayoutComponent {
  constructor(private authService: AuthService) {}

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
