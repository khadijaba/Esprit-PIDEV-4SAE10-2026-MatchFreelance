import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { EmailVerificationRequest } from '../../../models/auth.model';

@Component({
    selector: 'app-email-verification',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './email-verification.component.html',
})
export class EmailVerificationComponent implements OnInit {
    verificationForm!: FormGroup;
    isLoading = false;
    isVerified = false;
    email = '';

    constructor(
        private fb: FormBuilder,
        private authService: AuthService,
        private router: Router,
        private route: ActivatedRoute,
        private toastService: ToastService
    ) {}

    ngOnInit() {
        // Get email from query parameters (for email links)
        this.email = this.route.snapshot.queryParamMap.get('email') || '';

        this.verificationForm = this.fb.group({
            email: [this.email, [Validators.required, Validators.email]],
            verificationCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
        });
    }

    verifyEmail() {
        if (this.verificationForm.invalid) {
            this.verificationForm.markAllAsTouched();
            return;
        }

        this.isLoading = true;

        const request: EmailVerificationRequest = this.verificationForm.value;

        this.authService.verifyEmail(request).subscribe({
            next: (response) => {
                this.isLoading = false;
                this.isVerified = true;
                this.toastService.success('Email verified successfully!');
                
                // Redirect to login after successful verification
                setTimeout(() => {
                    this.router.navigate(['/login']);
                }, 2000);
            },
            error: (err) => {
                this.isLoading = false;
                const errorMsg = err.error || 'Verification failed. Please check your code and try again.';
                this.toastService.error(errorMsg);
            }
        });
    }

    resendVerification() {
        const email = this.verificationForm.get('email')?.value;
        if (!email) {
            this.toastService.error('Please enter your email address');
            return;
        }

        this.isLoading = true;

        this.authService.resendVerification({ email }).subscribe({
            next: (response) => {
                this.isLoading = false;
                this.toastService.success('Verification code sent successfully!');
            },
            error: (err) => {
                this.isLoading = false;
                const errorMsg = err.error || 'Failed to resend verification code';
                this.toastService.error(errorMsg);
            }
        });
    }

    goToLogin() {
        this.router.navigate(['/login']);
    }
}
