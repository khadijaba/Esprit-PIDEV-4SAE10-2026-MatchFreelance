import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NotificationMenuComponent } from '../notification-menu/notification-menu.component';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-client-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationMenuComponent],
  templateUrl: './client-layout.component.html',
})
export class ClientLayoutComponent {
  constructor(public auth: AuthService) {}

  logout() {
    this.auth.logout();
  }
}
