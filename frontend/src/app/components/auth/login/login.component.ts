import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { httpErrorMessage } from '../../../utils/http-error.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {
  loading = false;
  email = '';
  password = '';

  constructor(
    private auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    if (this.auth.isLoggedIn() && this.auth.getCurrentUser()) {
      this.auth.redirectByRole();
    }
  }

  submit(): void {
    const email = this.email.trim();
    if (!email || !this.password) {
      this.toast.error('Email et mot de passe requis');
      return;
    }
    this.loading = true;
    this.auth.login(email, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.toast.success('Connexion réussie');
        this.auth.redirectByRole();
      },
      error: (err: unknown) => {
        this.loading = false;
        const msg = httpErrorMessage(err, 'Connexion impossible');
        if (
          typeof err === 'object' &&
          err !== null &&
          'status' in err &&
          (err as { status: number }).status === 401
        ) {
          this.toast.error('Email ou mot de passe incorrect');
        } else {
          this.toast.error(msg);
        }
      },
    });
  }
}
