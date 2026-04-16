import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

@Component({
    selector: 'app-password-reset',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './password-reset.component.html',
})
export class PasswordResetComponent implements OnInit {
    resetForm!: FormGroup;
    isLoading = false;
    isEmailSent = false;
    currentEmail = '';

    constructor(
        private fb: FormBuilder,
        private authService: AuthService,
        private toastService: ToastService
    ) {}

    ngOnInit() {
        // Get current user's email from auth service
        this.currentEmail = this.authService.getUserEmail() || '';
        
        this.resetForm = this.fb.group({
            email: [this.currentEmail, [Validators.required, Validators.email]]
        });
    }

    requestPasswordReset() {
        if (this.resetForm.invalid) {
            this.resetForm.markAllAsTouched();
            return;
        }

        this.isLoading = true;

        const request = {
            email: this.resetForm.value.email
        };

        this.authService.requestPasswordReset(request).subscribe({
            next: (response) => {
                this.isLoading = false;
                this.isEmailSent = true;
                this.toastService.success('Password reset instructions sent to your email!');
            },
            error: (err) => {
                this.isLoading = false;
                const errorMsg = err.error || 'Failed to send password reset email';
                this.toastService.error(errorMsg);
            }
        });
    }

    close() {
        // This component will be used in a modal, so we'll handle closing via the parent
        const modal = document.getElementById('password-reset-modal');
        if (modal) {
            modal.classList.add('hidden');
        }
    }
}
