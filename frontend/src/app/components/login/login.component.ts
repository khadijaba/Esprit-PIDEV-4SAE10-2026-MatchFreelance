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

        let msg: string;
        if (typeof ProgressEvent !== 'undefined' && body instanceof ProgressEvent) {
          msg =
            status === 0
              ? 'Impossible de joindre le serveur (réseau ou proxy). Lancez le backend / la gateway et « ng serve » pour le proxy /api.'
              : 'Erreur réseau lors de la connexion.';
        } else if (status === 0) {
          msg =
            'Serveur injoignable. Vérifiez que le microservice User et la gateway tournent, et que vous utilisez bien l’URL du front avec proxy (ex. http://localhost:4200).';
        } else {
          const extracted =
            (typeof body === 'object' &&
              body != null &&
              !Array.isArray(body) &&
              (typeof (body as { error?: string }).error === 'string'
                ? (body as { error: string }).error
                : typeof (body as { message?: string }).message === 'string'
                  ? (body as { message: string }).message
                  : null)) ||
            (typeof body === 'string' ? body : null) ||
            (typeof err?.message === 'string' ? err.message : null) ||
            'Identifiants invalides.';
          msg = typeof extracted === 'string' ? extracted : 'Erreur de connexion.';
        }
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
    else if (role === 'CLIENT' || role === 'PROJECT_OWNER') this.router.navigateByUrl('/projets');
    else if (role === 'FREELANCER') this.router.navigateByUrl('/dashboard-freelancer');
    else this.router.navigateByUrl('/');
  }
}
