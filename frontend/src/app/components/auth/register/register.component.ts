import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { httpErrorMessage } from '../../../utils/http-error.util';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './register.component.html',
})
export class RegisterComponent implements OnInit {
  loading = false;
  selectedProfile: 'FREELANCER' | 'PROJECT_OWNER' = 'FREELANCER';
  email = '';
  password = '';
  fullName = '';

  constructor(
    private auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn() && this.auth.getCurrentUser()) {
      this.auth.redirectByRole();
    }
  }

  register(): void {
    const email = this.email.trim();
    if (!email || !this.password) {
      this.toast.error('Email et mot de passe requis');
      return;
    }
    if (this.password.length < 6) {
      this.toast.error('Le mot de passe doit contenir au moins 6 caractères');
      return;
    }
    const role = this.selectedProfile === 'PROJECT_OWNER' ? 'PROJECT_OWNER' : 'FREELANCER';
    this.loading = true;
    this.auth
      .register({
        email,
        password: this.password,
        fullName: this.fullName.trim() || undefined,
        role,
      })
      .subscribe({
        next: () => {
          this.loading = false;
          this.toast.success('Compte créé');
          this.auth.redirectByRole();
        },
        error: (err: unknown) => {
          this.loading = false;
          this.toast.error(httpErrorMessage(err, 'Inscription impossible'));
        },
      });
  }
}
