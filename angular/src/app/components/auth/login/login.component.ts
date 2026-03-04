import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { Role } from '../../../models/user.model';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {
    loginForm!: FormGroup;
    isLoading = false;
    errorMsg = '';

    constructor(
        private fb: FormBuilder,
        private authService: AuthService,
        private router: Router,
        private toastService: ToastService
    ) { }

    ngOnInit() {
        this.loginForm = this.fb.group({
            email: ['', [Validators.required, Validators.email]],
            password: ['', [Validators.required, Validators.minLength(6)]],
        });
    }

    onSubmit() {
        if (this.loginForm.invalid) {
            this.loginForm.markAllAsTouched();
            return;
        }

        this.isLoading = true;
        this.errorMsg = '';

        this.authService.login(this.loginForm.value).subscribe({
            next: (res) => {
                this.isLoading = false;
                this.toastService.success('Login successful');

                // Redirect based on role
                const role = this.authService.getUserRole();
                if (role === Role.ADMIN) {
                    this.router.navigate(['/admin/dashboard']);
                } else if (role === Role.PROJECT_OWNER) {
                    this.router.navigate(['/client']);
                } else {
                    // Freelancer or standard user typically goes to projects
                    this.router.navigate(['/projects']);
                }
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMsg = err.error || 'Invalid credentials. Please try again.';
                this.toastService.error('Login failed');
            }
        });
    }
}
