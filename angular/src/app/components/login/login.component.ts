import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });
  loading = false;

  fillDemo(role: 'client' | 'freelancer' | 'admin'): void {
    const creds = {
      client: { email: 'client@freelancehub.demo', password: 'client123' },
      freelancer: { email: 'freelancer1@freelancehub.demo', password: 'freelancer123' },
      admin: { email: 'admin@freelancehub.demo', password: 'admin123' },
    };
    this.form.setValue(creds[role]);
  }

  constructor() {
    if (this.auth.isAuthenticated()) {
      const u = this.auth.user();
      if (u?.role === 'ADMIN') this.router.navigate(['/admin']);
      else if (u?.role === 'CLIENT') this.router.navigate(['/client']);
      else if (u?.role === 'FREELANCER') this.router.navigate(['/projects']);
      else this.router.navigate(['/']);
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    const { email, password } = this.form.getRawValue();
    this.auth.login({ email: email.trim(), password }).subscribe({
      next: (user) => {
        this.loading = false;
        this.toast.success(`Welcome, ${user.name}!`);
        switch (user.role) {
          case 'ADMIN':
            this.router.navigate(['/admin']);
            break;
          case 'CLIENT':
            this.router.navigate(['/client']);
            break;
          case 'FREELANCER':
            this.router.navigate(['/projects']);
            break;
          default:
            this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.loading = false;
        this.toast.error(err.error?.message ?? 'Invalid email or password');
      },
    });
  }
}
