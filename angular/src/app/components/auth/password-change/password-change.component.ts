import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

export interface PasswordChangeRequest {
    email: string;
    oldPassword: string;
    newPassword: string;
    verificationCode: string;
}

@Component({
    selector: 'app-password-change',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './password-change.component.html',
})
export class PasswordChangeComponent implements OnInit {
    passwordForm!: FormGroup;
    codeForm!: FormGroup;
    isLoading = false;
    isPasswordChanged = false;
    currentStep = 1; // 1: password entry, 2: code verification
    currentEmail = '';

    passwordStrengthScore = 0;
    passwordStrengthLabel: 'Too weak' | 'Weak' | 'Okay' | 'Strong' = 'Too weak';

    constructor(
        private fb: FormBuilder,
        private authService: AuthService,
        private router: Router,
        private route: ActivatedRoute,
        private toastService: ToastService
    ) {}

    ngOnInit() {
        // Get current user's email
        this.currentEmail = this.authService.getUserEmail() || '';
        
        // Check if email is provided in query params
        const emailFromParams = this.route.snapshot.queryParamMap.get('email');
        if (emailFromParams) {
            this.currentEmail = emailFromParams;
        }

        // Step 1: Password form
        this.passwordForm = this.fb.group({
            email: [this.currentEmail, [Validators.required, Validators.email]],
            oldPassword: ['', [Validators.required]],
            newPassword: ['', [Validators.required, Validators.minLength(6)]],
            confirmPassword: ['', [Validators.required]]
        }, {
            validators: this.passwordMatchValidator
        });

        this.updatePasswordStrength(this.passwordForm.get('newPassword')?.value ?? '');
        this.passwordForm.get('newPassword')?.valueChanges.subscribe((value) => {
            this.updatePasswordStrength(value ?? '');
        });

        // Step 2: Code form
        this.codeForm = this.fb.group({
            verificationCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
        });
    }

    private updatePasswordStrength(password: string) {
        const score = this.calculatePasswordStrengthScore(password);
        this.passwordStrengthScore = score;
        this.passwordStrengthLabel = this.getPasswordStrengthLabel(score);
    }

    private calculatePasswordStrengthScore(password: string): number {
        if (!password) return 0;

        const lengthScore = password.length >= 12 ? 2 : password.length >= 8 ? 1 : 0;
        const hasLower = /[a-z]/.test(password);
        const hasUpper = /[A-Z]/.test(password);
        const hasNumber = /\d/.test(password);
        const hasSymbol = /[^A-Za-z\d]/.test(password);

        const variety = [hasLower, hasUpper, hasNumber, hasSymbol].filter(Boolean).length;
        const varietyScore = variety >= 3 ? 2 : variety >= 2 ? 1 : 0;

        const raw = lengthScore + varietyScore;
        return Math.max(0, Math.min(4, raw));
    }

    private getPasswordStrengthLabel(score: number): 'Too weak' | 'Weak' | 'Okay' | 'Strong' {
        if (score <= 1) return 'Too weak';
        if (score === 2) return 'Weak';
        if (score === 3) return 'Okay';
        return 'Strong';
    }

    get passwordStrengthBars(): number[] {
        return [1, 2, 3, 4];
    }

    get passwordStrengthBarClass(): string {
        if (this.passwordStrengthScore <= 1) return 'bg-red-500';
        if (this.passwordStrengthScore === 2) return 'bg-amber-500';
        if (this.passwordStrengthScore === 3) return 'bg-yellow-500';
        return 'bg-emerald-500';
    }

    passwordMatchValidator(form: FormGroup) {
        const newPassword = form.get('newPassword')?.value;
        const confirmPassword = form.get('confirmPassword')?.value;
        
        if (newPassword && confirmPassword && newPassword !== confirmPassword) {
            form.get('confirmPassword')?.setErrors({ passwordMismatch: true });
            return { passwordMismatch: true };
        }
        
        return null;
    }

    async submitPasswords() {
        if (this.passwordForm.invalid) {
            this.passwordForm.markAllAsTouched();
            return;
        }

        this.isLoading = true;

        // First, send verification code
        const email = this.passwordForm.value.email;
        this.authService.requestPasswordReset({ email }).subscribe({
            next: (response) => {
                this.isLoading = false;
                this.toastService.success('Verification code sent to your email!');
                this.currentStep = 2; // Move to code verification step
            },
            error: (err) => {
                this.isLoading = false;
                const errorMsg = err.error || 'Failed to send verification code';
                this.toastService.error(errorMsg);
            }
        });
    }

    changePassword() {
        if (this.codeForm.invalid) {
            this.codeForm.markAllAsTouched();
            return;
        }

        this.isLoading = true;

        const request: PasswordChangeRequest = {
            email: this.passwordForm.value.email,
            oldPassword: this.passwordForm.value.oldPassword,
            newPassword: this.passwordForm.value.newPassword,
            verificationCode: this.codeForm.value.verificationCode
        };

        this.authService.changePasswordWithCode(request).subscribe({
            next: (response) => {
                this.isLoading = false;
                this.isPasswordChanged = true;
                this.toastService.success('Password changed successfully!');
                
                // Redirect to dashboard after successful password change
                setTimeout(() => {
                    this.redirectToDashboard();
                }, 2000);
            },
            error: (err) => {
                this.isLoading = false;
                const errorMsg = err.error || 'Failed to change password';
                this.toastService.error(errorMsg);
            }
        });
    }

    goBackToStep1() {
        this.currentStep = 1;
        this.codeForm.reset();
    }

    redirectToDashboard() {
        const userRole = this.authService.getUserRole();
        switch (userRole) {
            case 'ADMIN':
                this.router.navigate(['/admin/dashboard']);
                break;
            case 'PROJECT_OWNER':
                this.router.navigate(['/client']);
                break;
            case 'FREELANCER':
                this.router.navigate(['/']);
                break;
            default:
                this.router.navigate(['/login']);
        }
    }

    goToDashboard() {
        this.redirectToDashboard();
    }
}
