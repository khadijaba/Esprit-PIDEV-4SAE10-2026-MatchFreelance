import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { FaceIdCaptureComponent } from '../face-id-capture/face-id-capture.component';
import { MF_VERIFY_PENDING_KEY } from '../../verify-pending-key';

type AccountSegment = 'FREELANCER' | 'PROJECT_OWNER' | 'ADMIN';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FaceIdCaptureComponent],
  templateUrl: './register.component.html',
  styles: [
    `
      .auth-pidev-bg {
        background: linear-gradient(160deg, #f1f5f9 0%, #e0e7ff 40%, #f8fafc 100%);
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
      .segment-active {
        box-shadow: 0 0 0 2px #4f46e5;
        background: #fff;
        color: #312e81;
      }
    `,
  ],
})
export class RegisterComponent {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  avatarFile: File | null = null;
  avatarPreview: string | null = null;
  showFaceHint = false;
  faceCaptureError = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private toast: ToastService
  ) {
    this.form = this.fb.group(
      {
        firstName: ['', Validators.required],
        lastName: ['', Validators.required],
        email: ['', [Validators.required, Validators.email]],
        address: ['', Validators.required],
        birthDate: ['', Validators.required],
        password: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', Validators.required],
        accountType: ['FREELANCER' as AccountSegment, Validators.required],
        faceDescriptor: [''],
      },
      { validators: RegisterComponent.passwordsMatch }
    );
  }

  private static passwordsMatch(group: AbstractControl): ValidationErrors | null {
    const p = group.get('password')?.value;
    const c = group.get('confirmPassword')?.value;
    if (p == null || c == null || p === '' || c === '') return null;
    return p === c ? null : { passwordMismatch: true };
  }

  setAccountType(t: AccountSegment): void {
    if (t === 'ADMIN') return;
    this.form.patchValue({ accountType: t });
  }

  onGoogle(): void {
    window.location.href = '/oauth2/authorization/google';
  }

  onAvatarChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.avatarFile = file ?? null;
    if (this.avatarPreview) {
      URL.revokeObjectURL(this.avatarPreview);
      this.avatarPreview = null;
    }
    if (file && file.type.startsWith('image/')) {
      this.avatarPreview = URL.createObjectURL(file);
    }
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.form.hasError('passwordMismatch')) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }
    const v = this.form.getRawValue();
    if (v.accountType === 'ADMIN') {
      this.errorMessage = 'Administrator accounts cannot be created from this form.';
      return;
    }

    const fd = new FormData();
    fd.append('firstName', v.firstName.trim());
    fd.append('lastName', v.lastName.trim());
    fd.append('email', v.email.trim());
    fd.append('password', v.password);
    fd.append('address', v.address.trim());
    fd.append('birthDate', v.birthDate);
    fd.append('role', v.accountType);
    const face = (v.faceDescriptor as string)?.trim();
    if (face) {
      fd.append('faceDescriptor', face);
    }
    if (this.avatarFile) {
      fd.append('file', this.avatarFile, this.avatarFile.name);
    }

    this.loading = true;
    this.auth.registerPidevMultipart(fd).subscribe({
      next: (res) => {
        this.loading = false;
        this.toast.success(
          'Compte créé. Un code à 6 chiffres vous est envoyé par e-mail — saisissez-le à l’étape suivante, puis connectez-vous.'
        );
        try {
          sessionStorage.setItem(
            MF_VERIFY_PENDING_KEY,
            JSON.stringify({ email: res.email, code: res.verificationCode ?? '' })
          );
        } catch {
          /* ignore quota / private mode */
        }
        void this.router.navigate(['/verify-email'], {
          queryParams: { email: res.email },
          state: { verificationCode: res.verificationCode ?? '' },
        });
      },
      error: (err) => {
        this.loading = false;
        const body = err?.error;
        this.errorMessage =
          typeof body === 'string' ? body : body?.message ?? err?.message ?? 'Registration failed.';
      },
    });
  }
}
