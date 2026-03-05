import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { JwtResponse, SignInRequest, SignUpRequest, EmailVerificationRequest, ResendVerificationRequest } from '../models/auth.model';
import { Role } from '../models/user.model';
import { Router } from '@angular/router';

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = `${environment.apiUrl}/auth`;
    private readonly TOKEN_KEY = 'auth_token';
    private readonly USER_ROLE_KEY = 'user_role';
    private readonly USER_EMAIL_KEY = 'user_email';

    constructor(private http: HttpClient, private router: Router) { }

    login(request: SignInRequest): Observable<JwtResponse> {
        return this.http.post<JwtResponse>(`${this.apiUrl}/signin`, request).pipe(
            tap(response => {
                if (response && response.token) {
                    this.setSession(response);
                }
            })
        );
    }

    register(request: SignUpRequest): Observable<any> {
        return this.http.post(`${this.apiUrl}/signup`, request, { responseType: 'text' });
    }

    logout(): void {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_ROLE_KEY);
        localStorage.removeItem(this.USER_EMAIL_KEY);
        this.router.navigate(['/login']);
    }

    private setSession(authResult: JwtResponse): void {
        localStorage.setItem(this.TOKEN_KEY, authResult.token);
        localStorage.setItem(this.USER_ROLE_KEY, authResult.role);
        localStorage.setItem(this.USER_EMAIL_KEY, authResult.email);
    }

    isLoggedIn(): boolean {
        const token = this.getToken();
        if (!token) return false;
        
        // Check if token is expired
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const now = Date.now() / 1000;
            return payload.exp > now;
        } catch (e) {
            return false;
        }
    }

    getToken(): string | null {
        return localStorage.getItem(this.TOKEN_KEY);
    }

    getUserRole(): Role | null {
        return localStorage.getItem(this.USER_ROLE_KEY) as Role | null;
    }

    getUserEmail(): string | null {
        return localStorage.getItem(this.USER_EMAIL_KEY);
    }

    verifyEmail(request: EmailVerificationRequest): Observable<string> {
        return this.http.post(`${this.apiUrl}/verify-email`, request, { responseType: 'text' });
    }

    resendVerification(request: ResendVerificationRequest): Observable<string> {
        return this.http.post(`${this.apiUrl}/resend-verification`, request, { responseType: 'text' });
    }

    requestPasswordReset(request: { email: string }): Observable<string> {
        return this.http.post(`${this.apiUrl}/reset-password/request`, request, { responseType: 'text' });
    }

    changePasswordWithCode(request: { email: string; oldPassword: string; newPassword: string; verificationCode: string }): Observable<string> {
        return this.http.post(`${this.apiUrl}/change-password-with-code`, request, { responseType: 'text' });
    }
}
