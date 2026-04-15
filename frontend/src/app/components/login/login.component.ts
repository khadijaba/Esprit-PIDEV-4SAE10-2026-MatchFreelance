import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { FaceIdCaptureComponent } from '../face-id-capture/face-id-capture.component';
import { MF_VERIFY_PENDING_KEY } from '../../verify-pending-key';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FaceIdCaptureComponent],
  templateUrl: './login.component.html',
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
export class LoginComponent implements OnInit {
  form: FormGroup;
  loading = false;
  faceLoading = false;
  errorMessage = '';
  showFace = false;
  faceDescriptor = '';
  faceCaptureError = '';
  /** Message après inscription PIDEV (compte souvent inactif jusqu’à vérif email). */
  postRegisterInfo = '';
  /** Afficher le lien vers la page de vérification (ex. 403 compte désactivé). */
  showVerifyEmailCta = false;

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

  ngOnInit(): void {
    const st = history.state as { registerNotice?: string } | null;
    if (st?.registerNotice) {
      this.postRegisterInfo = st.registerNotice;
      history.replaceState({ ...st, registerNotice: undefined }, '');
    }
  }

  onGoogle(): void {
    window.location.href = '/oauth2/authorization/google';
  }

  onFaceDescriptorFromCamera(csv: string): void {
    this.faceDescriptor = csv;
    this.faceCaptureError = '';
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.showVerifyEmailCta = false;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    const { email, password } = this.form.getRawValue();
    this.auth.loginPidev({ email, password }).subscribe({
      next: () => {
        this.loading = false;
        this.toast.success('Signed in successfully');
        this.navigateAfterLogin();
      },
      error: (err) => {
        this.loading = false;
        this.showVerifyEmailCta = false;
        const status = err?.status as number | undefined;
        const body = err?.error;
        let msg =
          (typeof body === 'object' && body != null && (body.error ?? body.message)) ||
          (typeof body === 'string' ? body : null) ||
          err?.message ||
          'E-mail ou mot de passe incorrect.';
        if (typeof msg !== 'string') msg = 'Erreur de connexion.';
        if (status === 403) {
          this.showVerifyEmailCta = true;
          const e = (email as string)?.trim();
          if (e) {
            try {
              let code = '';
              const raw = sessionStorage.getItem(MF_VERIFY_PENDING_KEY);
              if (raw) {
                try {
                  const j = JSON.parse(raw) as { email?: string; code?: string };
                  if (j.email === e && j.code) {
                    code = j.code;
                  }
                } catch {
                  /* ignore */
                }
              }
              sessionStorage.setItem(MF_VERIFY_PENDING_KEY, JSON.stringify({ email: e, code }));
            } catch {
              /* ignore */
            }
          }
          msg =
            'Ce compte n’est pas encore activé. Ouvrez l’e-mail de vérification (code à 6 chiffres), validez sur la page « Vérifier l’e-mail », puis reconnectez-vous.';
        }
        if (status === 405) {
          const hint = typeof body === 'object' && body != null && typeof body.hint === 'string' ? body.hint : '';
          msg =
            'Méthode non autorisée — utilisez ce formulaire pour vous connecter.' + (hint ? ` ${hint}` : '');
        }
        this.errorMessage = msg;
      },
    });
  }

  onFaceSignIn(): void {
    this.errorMessage = '';
    const email = this.form.get('email')?.value?.trim();
    if (!email || this.form.get('email')?.invalid) {
      this.form.get('email')?.markAsTouched();
      this.errorMessage = 'Enter a valid email above first.';
      return;
    }
    const desc = this.faceDescriptor.trim();
    if (!desc) return;
    this.faceLoading = true;
    this.auth.loginPidevFace(email, desc).subscribe({
      next: () => {
        this.faceLoading = false;
        this.toast.success('Face sign-in successful');
        this.navigateAfterLogin();
      },
      error: (err) => {
        this.faceLoading = false;
        const body = err?.error;
        this.errorMessage =
          typeof body === 'string' ? body : err?.message ?? 'Face not recognized or face login not set up.';
      },
    });
  }

  private navigateAfterLogin(): void {
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
  }
}
