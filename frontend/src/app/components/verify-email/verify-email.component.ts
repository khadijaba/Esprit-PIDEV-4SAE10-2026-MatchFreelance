import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { MF_VERIFY_PENDING_KEY } from '../../verify-pending-key';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './verify-email.component.html',
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
export class VerifyEmailComponent implements OnInit {
  form: FormGroup;
  loading = false;
  resendLoading = false;
  errorMessage = '';
  hintCode = '';
  /** Si l’e-mail est déjà connu (session / URL), on ne montre que le champ code. */
  showEmailField = true;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      verificationCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
    });
  }

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap.get('email');
    if (q) {
      this.form.patchValue({ email: q });
    }
    const st = history.state as { verificationCode?: string } | null;
    if (st?.verificationCode) {
      this.applyIncomingCode(st.verificationCode);
      history.replaceState({ ...st, verificationCode: undefined }, '');
    }
    const pendingRaw = sessionStorage.getItem(MF_VERIFY_PENDING_KEY);
    if (pendingRaw) {
      try {
        const j = JSON.parse(pendingRaw) as { email?: string; code?: string };
        if (j.email && !this.form.get('email')?.value) {
          this.form.patchValue({ email: j.email });
        }
        if (j.code) {
          this.applyIncomingCode(j.code);
        }
      } catch {
        /* ignore */
      }
    }
    this.syncEmailFieldVisibility();
  }

  /** Affiche le champ e-mail (ex. autre compte). */
  revealEmailField(): void {
    this.showEmailField = true;
  }

  private syncEmailFieldVisibility(): void {
    const ctrl = this.form.get('email');
    const v = (ctrl?.value as string)?.trim() ?? '';
    this.showEmailField = !(v.length > 0 && ctrl?.valid === true);
  }

  private applyIncomingCode(code: string): void {
    const c = code.trim();
    if (!c) {
      return;
    }
    this.hintCode = c;
    this.form.patchValue({ verificationCode: c });
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, verificationCode } = this.form.getRawValue();
    this.loading = true;
    this.auth.verifyEmail(email.trim(), verificationCode.trim()).subscribe({
      next: (msg) => {
        this.loading = false;
        sessionStorage.removeItem(MF_VERIFY_PENDING_KEY);
        this.toast.success(msg);
        void this.router.navigate(['/login'], {
          state: {
            registerNotice:
              'E-mail vérifié. Vous pouvez maintenant vous connecter (Sign in) avec le même e-mail et mot de passe.',
          },
        });
      },
      error: (err) => {
        this.loading = false;
        const body = err?.error;
        this.errorMessage = typeof body === 'string' ? body : err?.message ?? 'Verification failed.';
      },
    });
  }

  resend(): void {
    const email = this.form.get('email')?.value?.trim();
    if (!email || this.form.get('email')?.invalid) {
      this.form.get('email')?.markAsTouched();
      this.errorMessage = 'Indiquez d’abord un e-mail valide.';
      return;
    }
    this.errorMessage = '';
    this.resendLoading = true;
    this.auth.resendVerificationEmail(email).subscribe({
      next: (msg) => {
        this.resendLoading = false;
        this.toast.success(msg);
      },
      error: (err) => {
        this.resendLoading = false;
        const body = err?.error;
        this.errorMessage = typeof body === 'string' ? body : err?.message ?? 'Resend failed.';
      },
    });
  }
}
