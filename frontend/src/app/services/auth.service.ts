import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthResponse, LoginRequest, RegisterRequest, User, UserProfile } from '../models/auth.model';

const API = '/api/users';
const TOKEN_KEY = 'mf_token';
const USER_KEY = 'mf_user';

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
      })
    );
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

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
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
}
