import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { UserRole } from '../../models/auth.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  roles: { value: UserRole; label: string }[] = [
    { value: 'FREELANCER', label: 'Freelancer' },
    { value: 'CLIENT', label: 'Client' },
    { value: 'ADMIN', label: 'Admin' },
  ];

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private toast: ToastService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      fullName: [''],
      role: ['FREELANCER' as UserRole, Validators.required],
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.toast.success('Inscription réussie');
        this.router.navigate(['/admin']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.error ?? err?.message ?? 'Erreur lors de l\'inscription.';
      },
    });
  }
}
