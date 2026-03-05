import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
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
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.toast.success('Connexion réussie');
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] ?? history.state?.['returnUrl'];
        if (returnUrl) {
          this.router.navigateByUrl(returnUrl);
          return;
        }
        const role = this.auth.getStoredUser()?.role;
        if (role === 'ADMIN') this.router.navigateByUrl('/admin');
        else if (role === 'CLIENT') this.router.navigateByUrl('/dashboard-client');
        else if (role === 'FREELANCER') this.router.navigateByUrl('/dashboard-freelancer');
        else this.router.navigateByUrl('/');
      },
      error: (err) => {
        this.loading = false;
        const body = err?.error;
        let msg =
          (typeof body === 'object' && body != null && (body.error ?? body.message)) ||
          (typeof body === 'string' ? body : null) ||
          err?.message ||
          'Email ou mot de passe incorrect.';
        if (typeof msg !== 'string') msg = 'Erreur de connexion.';
        this.errorMessage = msg;
      },
    });
  }
}
