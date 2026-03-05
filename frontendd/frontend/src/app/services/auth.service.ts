import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  UserRole,
} from '../models/auth.model';
import { User } from '../models/user.model';

const TOKEN_KEY = 'matchfreelance_token';
const USER_KEY = 'matchfreelance_user';
const ROLE_KEY = 'matchfreelance_role';

@Injectable({ providedIn: 'root' })
export class AuthService {
  /** Toutes les requêtes passent par la Gateway (proxy /api → 8086) */
  private readonly api = '/api/users';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  /** Backend: POST /users/auth/login (Gateway: /api/users/auth/login) */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/login`, credentials)
      .pipe(tap((res) => this.saveSession(res)));
  }

  /** Backend: POST /users/auth/register (Gateway: /api/users/auth/register). Payload: email, password, fullName, role. */
  register(data: RegisterRequest): Observable<AuthResponse> {
    const body = this.toRegisterBody(data);
    return this.http
      .post<AuthResponse>(`${this.api}/auth/register`, body)
      .pipe(tap((res) => this.saveSession(res)));
  }

  /** Backend attend role = FREELANCER | CLIENT | ADMIN (pas PROJECT_OWNER). On envoie CLIENT pour "Project Owner". */
  private toRegisterBody(data: RegisterRequest): Record<string, unknown> {
    const fullName = [data.firstName, data.lastName].filter(Boolean).join(' ').trim() || data.username;
    const role = data.role === 'PROJECT_OWNER' ? 'CLIENT' : data.role;
    return {
      email: data.email,
      password: data.password,
      fullName: fullName || data.email,
      role,
    };
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(ROLE_KEY);
    this.router.navigate(['/']);
  }

  private saveSession(res: AuthResponse): void {
    if (res.token) localStorage.setItem(TOKEN_KEY, res.token);
    const role = this.normalizeRole(res.role ?? res.user?.role);
    if (role) localStorage.setItem(ROLE_KEY, role);
    const user = res.user ?? this.userFromBackendResponse(res);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  /** Backend renvoie { token, userId, email, fullName, role } sans objet "user". On construit l'objet user. */
  private userFromBackendResponse(res: AuthResponse): User | null {
    const raw = res as unknown as { userId?: number; email?: string; fullName?: string; role?: string };
    if (raw.userId == null && !res.user?.id) return null;
    return {
      id: raw.userId ?? res.user?.id ?? 0,
      email: raw.email ?? res.user?.email,
      username: raw.fullName ?? res.user?.username,
      role: this.normalizeRole(raw.role ?? res.role) ?? undefined,
    };
  }

  /** Backend utilise CLIENT pour "project owner". Côté front on garde PROJECT_OWNER pour isProjectOwner(). */
  private normalizeRole(role: string | undefined): string | null {
    if (!role) return null;
    return role === 'CLIENT' ? 'PROJECT_OWNER' : role;
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getCurrentUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      return null;
    }
  }

  getRole(): UserRole | null {
    const r = localStorage.getItem(ROLE_KEY);
    return (r as UserRole) || null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isFreelancer(): boolean {
    return this.getRole() === 'FREELANCER';
  }

  isProjectOwner(): boolean {
    return this.getRole() === 'PROJECT_OWNER';
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  /** Redirection après login selon le rôle */
  redirectByRole(): void {
    const role = this.getRole();
    if (role === 'FREELANCER') {
      this.router.navigate(['/freelancer/skills']);
    } else if (role === 'PROJECT_OWNER') {
      this.router.navigate(['/project-owner/projects']);
    } else if (role === 'ADMIN') {
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.router.navigate(['/']);
    }
  }
}
