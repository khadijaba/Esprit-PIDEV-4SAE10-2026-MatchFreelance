import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;

  constructor(private auth: AuthService) {}

  submit() {
    if (!this.email || !this.password) {
      return;
    }
    this.loading = true;
    this.auth.login({ email: this.email, password: this.password });
    // auth service handles navigation & toasts; loading flag will be reset on next CD cycle if needed.
    setTimeout(() => (this.loading = false), 500);
  }
}

