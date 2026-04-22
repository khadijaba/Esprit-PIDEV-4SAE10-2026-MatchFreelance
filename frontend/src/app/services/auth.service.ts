import { Injectable, Injector } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap } from 'rxjs';
import { Router } from '@angular/router';
import { UserProfile, UserRole } from '../models/auth.model';
import { User } from '../models/user.model';

const TOKEN_KEY = 'matchfreelance_token';
const USER_KEY = 'matchfreelance_user';
const ROLE_KEY = 'matchfreelance_role';

/** Réponse du microservice User (AuthResponse Java). */
interface BackendAuthResponse {
  token: string;
  userId: number;
  email: string;
  fullName?: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = '/api/users';

  constructor(
    private injector: Injector,
    private router: Router
  ) {}

  private get http(): HttpClient {
    return this.injector.get(HttpClient);
  }

  login(email: string, password: string): Observable<BackendAuthResponse> {
    return this.http.post<BackendAuthResponse>(`${this.api}/login`, { email, password })
      .pipe(tap((res) => this.persistAuth(res)));
  }

  register(payload: {
    email: string;
    password: string;
    fullName?: string;
    role: 'FREELANCER' | 'PROJECT_OWNER';
  }): Observable<BackendAuthResponse> {
    return this.http
      .post<BackendAuthResponse>(`${this.api}/register`, payload)
      .pipe(tap((res) => this.persistAuth(res)));
  }

  private persistAuth(res: BackendAuthResponse): void {
    if (!res?.token || res.userId == null) {
      throw new Error('Réponse du serveur incomplète (token ou identifiant manquant).');
    }
    localStorage.setItem(TOKEN_KEY, res.token);

    let rawRole = String(res.role ?? '').trim();
    if (!rawRole) {
      try {
        const payload = JSON.parse(atob(res.token.split('.')[1])) as { role?: string };
        rawRole = (payload.role ?? '').trim();
      } catch {
        /* ignore */
      }
    }
    const role = this.normalizeRole(rawRole);
    localStorage.removeItem(ROLE_KEY);
    if (role) {
      localStorage.setItem(ROLE_KEY, role);
    }
    const mapped: User = {
      id: Number(res.userId),
      userId: Number(res.userId),
      email: res.email,
      username: res.fullName ?? res.email,
      fullName: res.fullName,
      role: role ?? rawRole,
    };
    localStorage.setItem(USER_KEY, JSON.stringify(mapped));
  }

  refreshLocalProfile(): Observable<void> {
    return this.http.get<Record<string, unknown>>(`${this.api}/me`).pipe(
      tap((body) => {
        const id = body['localUserId'] as number;
        const email = (body['email'] as string) ?? '';
        const fullName = body['fullName'] as string | undefined;
        const rawRole = String(body['localRole'] ?? '');
        const role = this.normalizeRole(rawRole);
        if (role) localStorage.setItem(ROLE_KEY, role);
        const mapped: User = {
          id: Number(id),
          userId: Number(id),
          email,
          fullName,
          username: fullName || email,
          role: role ?? rawRole,
        };
        localStorage.setItem(USER_KEY, JSON.stringify(mapped));
      }),
      map(() => undefined)
    );
  }

  private clearLocalProfile(): void {
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(TOKEN_KEY);
  }

  getToken(): string | null {
    const t = localStorage.getItem(TOKEN_KEY);
    if (!t || this.isTokenExpired(t)) {
      if (t) this.clearLocalProfile();
      return null;
    }
    return t;
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1])) as { exp?: number };
      if (!payload.exp) return false;
      return payload.exp * 1000 < Date.now() + 5000;
    } catch {
      return true;
    }
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

  /** Alias de compatibilité (anciens composants). */
  getStoredUser(): User | null {
    return this.getCurrentUser();
  }

  getRole(): UserRole | null {
    const raw = localStorage.getItem(ROLE_KEY);
    if (!raw) return null;
    const u = raw.trim().toUpperCase();
    if (u === 'CLIENT') {
      localStorage.setItem(ROLE_KEY, 'PROJECT_OWNER');
      const userJson = localStorage.getItem(USER_KEY);
      if (userJson) {
        try {
          const user = JSON.parse(userJson) as User;
          user.role = 'PROJECT_OWNER';
          localStorage.setItem(USER_KEY, JSON.stringify(user));
        } catch {
          /* ignore */
        }
      }
      return 'PROJECT_OWNER';
    }
    return (raw as UserRole) || null;
  }

  /** Liste des utilisateurs (optionnel : filtrer par rôle). */
  getUsers(role?: 'ADMIN' | 'FREELANCER' | 'PROJECT_OWNER'): Observable<User[]> {
    if (role) {
      return this.http.get<User[]>(this.api, { params: { role } });
    }
    return this.http.get<User[]>(this.api);
  }

  /** Profil complet de l'utilisateur connecté. */
  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.api}/me/profile`);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  ensureFreshTokenIfNeeded(): Promise<void> {
    if (!this.getToken()) return Promise.resolve();
    return Promise.resolve();
  }

  logout(): void {
    this.clearLocalProfile();
    void this.router.navigate(['/']);
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

  redirectByRole(): void {
    const role = this.getRole();
    if (role === 'FREELANCER') {
      void this.router.navigate(['/freelancer/skills']);
    } else if (role === 'PROJECT_OWNER') {
      void this.router.navigate(['/project-owner/projects']);
    } else if (role === 'ADMIN') {
      void this.router.navigate(['/admin/dashboard']);
    } else {
      void this.router.navigate(['/']);
    }
  }

  private normalizeRole(role: string | undefined): string | null {
    if (!role) return null;
    const u = role.trim().toUpperCase();
    if (u === 'CLIENT') return 'PROJECT_OWNER';
    if (u === 'FREELANCER' || u === 'PROJECT_OWNER' || u === 'ADMIN') return u;
    return role.trim();
  }
}
