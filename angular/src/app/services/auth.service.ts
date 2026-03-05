import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, User } from '../models/auth.model';

const STORAGE_KEY = 'auth';
const API = '/api/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _user = signal<User | null>(null);
  private readonly _token = signal<string | null>(null);

  readonly user = this._user.asReadonly();
  readonly token = this._token.asReadonly();
  readonly isAuthenticated = computed(() => !!this._token());
  readonly currentUserId = computed(() => this._user()?.id ?? null);

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadFromStorage();
  }

  private loadFromStorage(): void {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      try {
        const data = JSON.parse(raw) as LoginResponse;
        if (data.token && data.id) {
          this._token.set(data.token);
          this._user.set({
            id: data.id,
            email: data.email,
            name: data.name,
            role: data.role,
          });
        }
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    }
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API}/login`, credentials).pipe(
      tap((res) => {
        this._token.set(res.token);
        this._user.set({
          id: res.id,
          email: res.email,
          name: res.name,
          role: res.role,
        });
        localStorage.setItem(STORAGE_KEY, JSON.stringify(res));
      })
    );
  }

  logout(): void {
    this._token.set(null);
    this._user.set(null);
    localStorage.removeItem(STORAGE_KEY);
    this.router.navigate(['/login']);
  }

  hasRole(role: User['role']): boolean {
    return this._user()?.role === role;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  isClient(): boolean {
    return this.hasRole('CLIENT');
  }

  isFreelancer(): boolean {
    return this.hasRole('FREELANCER');
  }

  getAuthHeader(): string | null {
    const t = this._token();
    return t ? `Bearer ${t}` : null;
  }
}
