import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { User, UserRole } from '../models/user.model';
import { ToastService } from './toast.service';

interface LoginRequest {
  email: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'app_current_user';
  private _user = signal<User | null>(null);

  currentUser = computed(() => this._user());

  constructor(
    private http: HttpClient,
    private router: Router,
    private toast: ToastService
  ) {
    const raw = sessionStorage.getItem(this.storageKey);
    if (raw) {
      try {
        this._user.set(JSON.parse(raw));
      } catch {
        sessionStorage.removeItem(this.storageKey);
      }
    }
  }

  login(credentials: LoginRequest) {
    this.http.post<User>('/api/users/login', credentials).subscribe({
      next: (user) => {
        this._user.set(user);
        sessionStorage.setItem(this.storageKey, JSON.stringify(user));
        this.toast.success('Signed in as ' + user.email);
        this.redirectByRole(user.role);
      },
      error: (err) => {
        this.toast.error(err?.error?.message || 'Invalid email or password');
      },
    });
  }

  logout() {
    this._user.set(null);
    sessionStorage.removeItem(this.storageKey);
    this.router.navigate(['/login']);
  }

  private redirectByRole(role: UserRole) {
    if (role === 'ADMIN') {
      this.router.navigate(['/admin']);
    } else if (role === 'CLIENT') {
      this.router.navigate(['/client']);
    } else {
      this.router.navigate(['/freelancer']);
    }
  }
}

