import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { LoginRequest } from '../../../models/auth.model';
import { getBackendErrorMessage } from '../../../utils/http-error.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  credentials: LoginRequest = { email: '', password: '' };
  loading = false;

  constructor(
    private auth: AuthService,
    private toast: ToastService
  ) {}

  onSubmit(): void {
    if (!this.credentials.email?.trim() || !this.credentials.password) {
      this.toast.error('Email et mot de passe requis');
      return;
    }
    this.loading = true;
    this.auth.login(this.credentials).subscribe({
      next: () => {
        this.toast.success('Connexion réussie');
        this.auth.redirectByRole();
      },
      error: (err) => {
        this.loading = false;
        if (err && typeof (err as { status?: number }).status === 'number' && (err as { status: number }).status === 500) {
          console.error('[Login 500] Backend response:', (err as { error?: unknown }).error);
        }
        const msg = getBackendErrorMessage(err, 'Email ou mot de passe incorrect. En cas d\'erreur 500, consultez les logs du backend.');
        this.toast.error(msg);
      },
    });
  }
}
