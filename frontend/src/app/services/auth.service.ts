import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, switchMap, tap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthResponse, LoginRequest, RegisterRequest, User, UserProfile, UserRole } from '../models/auth.model';

const API = '/api/users';

export interface PidevSignupResponse {
  message: string;
  email: string;
  verificationCode?: string;
}

/** Réponse JWT du microservice User PIDEV (POST /api/auth/signin, /api/auth/signin-face). */
export interface JwtResponseDto {
  token: string;
  email: string;
  role: 'FREELANCER' | 'PROJECT_OWNER' | 'ADMIN';
  message?: string;
}
const TOKEN_KEY = 'mf_token';
const USER_KEY = 'mf_user';

/** Ancien ID persisté pour démo hors-ligne ; doit être aligné sur la session ou supprimé à la déconnexion. */
export const FREELANCER_ID_STORAGE_KEY = 'freelancerId';

function syncFreelancerIdLocalStorage(res: AuthResponse): void {
  if (res.role === 'FREELANCER' && res.userId != null) {
    localStorage.setItem(FREELANCER_ID_STORAGE_KEY, String(res.userId));
  } else {
    localStorage.removeItem(FREELANCER_ID_STORAGE_KEY);
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/login`, body).pipe(
      tap((res) => {
        sessionStorage.setItem(TOKEN_KEY, res.token);
        sessionStorage.setItem(USER_KEY, JSON.stringify({
          userId: res.userId,
          email: res.email,
          fullName: res.fullName,
          role: res.role,
        }));
        syncFreelancerIdLocalStorage(res);
      })
    );
  }

  /** Connexion PIDEV (même backend que le projet Esprit User) : POST /api/auth/signin puis hydratation du profil MatchFreelance. */
  loginPidev(body: LoginRequest): Observable<AuthResponse> {
    return this.http.post<JwtResponseDto>('/api/auth/signin', body).pipe(
      switchMap((jwt) => this.hydrateSessionAfterPidevJwt(jwt))
    );
  }

  /** Connexion faciale PIDEV : descripteur 128 valeurs séparées par des virgules. */
  loginPidevFace(email: string, faceDescriptor: string): Observable<AuthResponse> {
    return this.http
      .post<JwtResponseDto>('/api/auth/signin-face', { email, faceDescriptor })
      .pipe(switchMap((jwt) => this.hydrateSessionAfterPidevJwt(jwt)));
  }

  private mapPidevRoleToFrontend(r: string): UserRole {
    if (r === 'PROJECT_OWNER') return 'CLIENT';
    return r as UserRole;
  }

  private hydrateSessionAfterPidevJwt(jwt: JwtResponseDto): Observable<AuthResponse> {
    const role = this.mapPidevRoleToFrontend(jwt.role);
    sessionStorage.setItem(TOKEN_KEY, jwt.token);
    sessionStorage.setItem(
      USER_KEY,
      JSON.stringify({
        userId: 0,
        email: jwt.email,
        fullName: null,
        role,
      })
    );
    return this.getProfile().pipe(
      tap((profile) => {
        const res: AuthResponse = {
          token: jwt.token,
          userId: profile.userId,
          email: profile.email,
          fullName: profile.fullName,
          role: profile.role,
        };
        sessionStorage.setItem(TOKEN_KEY, res.token);
        sessionStorage.setItem(USER_KEY, JSON.stringify({
          userId: res.userId,
          email: res.email,
          fullName: res.fullName,
          role: res.role,
        }));
        syncFreelancerIdLocalStorage(res);
      }),
      map(
        (profile): AuthResponse => ({
          token: jwt.token,
          userId: profile.userId,
          email: profile.email,
          fullName: profile.fullName,
          role: profile.role,
        })
      )
    );
  }

   /** Inscription PIDEV : multipart POST /api/auth/signup (réponse JSON ; verificationCode si activé côté serveur). */
  registerPidevMultipart(formData: FormData): Observable<PidevSignupResponse> {
    return this.http.post<PidevSignupResponse>('/api/auth/signup', formData);
  }

  verifyEmail(email: string, verificationCode: string): Observable<string> {
    return this.http.post('/api/auth/verify-email', { email, verificationCode }, { responseType: 'text' });
  }

  resendVerificationEmail(email: string): Observable<string> {
    return this.http.post('/api/auth/resend-verification', { email }, { responseType: 'text' });
  }

  register(body: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/auth/register`, body).pipe(
      tap((res) => {
        sessionStorage.setItem(TOKEN_KEY, res.token);
        sessionStorage.setItem(USER_KEY, JSON.stringify({
          userId: res.userId,
          email: res.email,
          fullName: res.fullName,
          role: res.role,
        }));
        syncFreelancerIdLocalStorage(res);
      })
    );
  }

  getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  getStoredUser(): { userId: number; email: string; fullName: string | null; role: string } | null {
    const raw = sessionStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  /** Vide la session sans rediriger (utile pour l'intercepteur 401). */
  clearSession(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    localStorage.removeItem(FREELANCER_ID_STORAGE_KEY);
  }

  logout(): void {
    this.clearSession();
    this.router.navigate(['/login']);
  }

  /** Liste des utilisateurs (optionnel : filtrer par rôle). */
  getUsers(role?: 'ADMIN' | 'FREELANCER' | 'CLIENT'): Observable<User[]> {
    if (role) {
      return this.http.get<User[]>(API, { params: { role } });
    }
    return this.http.get<User[]>(API);
  }

  /** Profil complet de l'utilisateur connecté (nécessite token). */
  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${API}/me/profile`);
  }

  /** Supprime le compte connecté (nécessite token). */
  deleteMyAccount(): Observable<void> {
    return this.http.delete<void>(`${API}/me`);
  }

  /** Demande de réinitialisation PIDEV (POST /api/auth/reset-password/request). */
  requestPasswordReset(email: string): Observable<string> {
    return this.http.post('/api/auth/reset-password/request', { email }, { responseType: 'text' });
  }

  /** Nouveau mot de passe après code 6 chiffres (e-mail « mot de passe oublié »). */
  completePasswordReset(body: {
    email: string;
    verificationCode: string;
    newPassword: string;
    confirmPassword: string;
  }): Observable<string> {
    return this.http.post('/api/auth/reset-password/complete', body, { responseType: 'text' });
  }
}
