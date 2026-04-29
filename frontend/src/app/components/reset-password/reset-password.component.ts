import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
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
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      verificationCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap.get('email');
    if (q) {
      this.form.patchValue({ email: q });
    }
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, verificationCode, newPassword, confirmPassword } = this.form.getRawValue();
    if (newPassword !== confirmPassword) {
      this.errorMessage = 'Les deux mots de passe ne correspondent pas.';
      return;
    }
    this.loading = true;
    this.auth
      .completePasswordReset({
        email: email.trim(),
        verificationCode: String(verificationCode).trim(),
        newPassword,
        confirmPassword,
      })
      .subscribe({
        next: (msg) => {
          this.loading = false;
          this.toast.success(msg);
          void this.router.navigate(['/login']);
        },
        error: (err) => {
          this.loading = false;
          const body = err?.error;
          this.errorMessage = typeof body === 'string' ? body : err?.message ?? 'Échec de la réinitialisation.';
        },
      });
  }
}
