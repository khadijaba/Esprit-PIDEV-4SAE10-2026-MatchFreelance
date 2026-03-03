import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { RegisterRequest, UserRole } from '../../../models/auth.model';
import { getBackendErrorMessage } from '../../../utils/http-error.util';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  model: RegisterRequest = {
    email: '',
    password: '',
    username: '',
    firstName: '',
    lastName: '',
    role: 'FREELANCER',
  };
  confirmPassword = '';
  loading = false;
  roles: { value: UserRole; label: string }[] = [
    { value: 'FREELANCER', label: 'Freelancer' },
    { value: 'PROJECT_OWNER', label: 'Project Owner' },
    { value: 'ADMIN', label: 'Admin' },
  ];

  constructor(
    private auth: AuthService,
    private toast: ToastService
  ) {}

  onSubmit(): void {
    if (!this.model.email?.trim()) {
      this.toast.error('Email requis');
      return;
    }
    if (!this.model.password || this.model.password.length < 6) {
      this.toast.error('Mot de passe d\'au moins 6 caractères');
      return;
    }
    if (this.model.password !== this.confirmPassword) {
      this.toast.error('Les mots de passe ne correspondent pas');
      return;
    }
    if (!this.model.username?.trim()) {
      this.toast.error('Nom d\'utilisateur requis');
      return;
    }
    this.loading = true;
    this.auth.register(this.model).subscribe({
      next: () => {
        this.toast.success('Compte créé avec succès');
        this.auth.redirectByRole();
      },
      error: (err) => {
        this.loading = false;
        const msg = getBackendErrorMessage(err);
        this.toast.error(msg);
      },
    });
  }
}
