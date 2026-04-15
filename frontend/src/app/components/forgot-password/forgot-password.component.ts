import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styles: [
    `
      .auth-pidev-bg {
        background: linear-gradient(160deg, #f1f5f9 0%, #e0e7ff 45%, #f8fafc 100%);
      }
      .auth-pidev-input {
        background: #f1f5f9;
        border: none;
      }
      .auth-pidev-input:focus {
        outline: 2px solid #818cf8;
        outline-offset: 0;
        background: #fff;
      }
    `,
  ],
})
export class ForgotPasswordComponent {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private toast: ToastService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.auth.requestPasswordReset(this.form.get('email')!.value.trim()).subscribe({
      next: (msg) => {
        this.loading = false;
        this.successMessage = msg;
        this.toast.success('Si le compte existe, un e-mail avec un code a été envoyé.');
      },
      error: (err) => {
        this.loading = false;
        const body = err?.error;
        this.errorMessage = typeof body === 'string' ? body : err?.message ?? 'Request failed.';
      },
    });
  }
}
