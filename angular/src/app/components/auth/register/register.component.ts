import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { Role } from '../../../models/user.model';

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './register.component.html',
})
export class RegisterComponent implements OnInit {
    registerForm!: FormGroup;
    isLoading = false;
    errorMsg = '';

    // Available roles for registration
    roles = [
        { value: Role.FREELANCER, label: 'Freelancer' },
        { value: Role.PROJECT_OWNER, label: 'Project Owner' },
        { value: Role.ADMIN, label: 'Administrator' }
    ];

    constructor(
        private fb: FormBuilder,
        private authService: AuthService,
        private router: Router,
        private toastService: ToastService
    ) { }

  ngOnInit() {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      address: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],  // ✅ Add this
      birthDate: ['', [Validators.required]],
      role: [Role.FREELANCER, [Validators.required]]
    });
  }

    onSubmit() {
        if (this.registerForm.invalid) {
            this.registerForm.markAllAsTouched();
            return;
        }

        this.isLoading = true;
        this.errorMsg = '';

        const formValue = this.registerForm.value;

        // Ensure birthDate format is YYYY-MM-DD
        const date = new Date(formValue.birthDate);
        const formattedDate = date.toISOString().split('T')[0];

        const request = {
            ...formValue,
            birthDate: formattedDate
        };

        this.authService.register(request).subscribe({
            next: (res) => {
                this.isLoading = false;
                this.toastService.success('Registration successful! Please check your email for verification.');
                this.router.navigate(['/verify-email']);
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMsg = err.error || 'Registration failed. Please try again.';
                this.toastService.error('Registration failed');
            }
        });
    }
}
