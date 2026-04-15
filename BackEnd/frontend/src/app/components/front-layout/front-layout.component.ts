import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-front-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './front-layout.component.html',
})
export class FrontLayoutComponent {
  currentYear = new Date().getFullYear();

  constructor(public auth: AuthService) {}

  logout() {
    this.auth.logout();
  }
}
