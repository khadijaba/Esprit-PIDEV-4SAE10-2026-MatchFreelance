import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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

type AccountSegment = 'FREELANCER' | 'PROJECT_OWNER' | 'ADMIN';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FaceIdCaptureComponent],
  templateUrl: './register.component.html',
})
export class RegisterComponent implements OnInit {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  avatarFile: File | null = null;
  avatarPreview: string | null = null;
  showFaceHint = false;
  faceCaptureError = '';

  passwordStrengthScore = 0;
  passwordStrengthLabel: 'Too weak' | 'Weak' | 'Okay' | 'Strong' = 'Too weak';

  readonly accountSegments: { value: Exclude<AccountSegment, 'ADMIN'>; label: string }[] = [
    { value: 'FREELANCER', label: 'Freelancer' },
    { value: 'PROJECT_OWNER', label: 'Project Owner' },
  ];

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
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

  ngOnInit(): void {
    const roleParam = this.route.snapshot.queryParamMap.get('role')?.toLowerCase() ?? '';
    if (roleParam === 'freelancer' || roleParam === 'freelance') {
      this.form.patchValue({ accountType: 'FREELANCER' });
    } else if (
      roleParam === 'project_owner' ||
      roleParam === 'projectowner' ||
      roleParam === 'client' ||
      roleParam === 'owner'
    ) {
      this.form.patchValue({ accountType: 'PROJECT_OWNER' });
    }

    const pwd = this.form.get('password');
    this.updatePasswordStrength(pwd?.value ?? '');
    pwd?.valueChanges.subscribe((v) => this.updatePasswordStrength(v ?? ''));
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

  get faceDescriptorSaved(): boolean {
    return String(this.form.get('faceDescriptor')?.value ?? '').trim().length > 0;
  }

  private updatePasswordStrength(password: string): void {
    const score = RegisterComponent.calculatePasswordStrengthScore(password);
    this.passwordStrengthScore = score;
    this.passwordStrengthLabel = RegisterComponent.passwordStrengthLabelFor(score);
  }

  private static calculatePasswordStrengthScore(password: string): number {
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

  private static passwordStrengthLabelFor(score: number): 'Too weak' | 'Weak' | 'Okay' | 'Strong' {
    if (score <= 1) return 'Too weak';
    if (score === 2) return 'Weak';
    if (score === 3) return 'Okay';
    return 'Strong';
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

  removeAvatar(): void {
    this.avatarFile = null;
    if (this.avatarPreview) {
      URL.revokeObjectURL(this.avatarPreview);
      this.avatarPreview = null;
    }
  }

  onFaceDescriptorReady(csv: string): void {
    this.form.patchValue({ faceDescriptor: csv });
    this.faceCaptureError = '';
  }

  clearFaceDescriptor(): void {
    this.form.patchValue({ faceDescriptor: '' });
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
      next: () => {
        this.loading = false;
        this.toast.success('Compte créé avec succès. Connectez-vous maintenant.');
        void this.router.navigate(['/login'], {
          state: { registerNotice: 'Compte créé. Vous pouvez vous connecter.' },
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
