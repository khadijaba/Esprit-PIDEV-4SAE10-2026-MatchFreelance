import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
    selector: 'app-oauth2-redirect',
    standalone: true,
    template: `
    <div class="h-screen flex flex-col items-center justify-center bg-slate-50">
      <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-600 mb-4"></div>
      <p class="text-slate-600 font-medium tracking-wide">Securely logging you in with Google...</p>
    </div>
  `
})
export class Oauth2RedirectComponent implements OnInit {

    constructor(private route: ActivatedRoute, private router: Router, private authService: AuthService) { }

    ngOnInit() {
        this.route.queryParams.subscribe(params => {
            const token = params['token'];
            if (token) {
                localStorage.setItem('auth_token', token);

                // Parse token payload dynamically to decide where to route
                const payloadStr = atob(token.split('.')[1]);
                const payload = JSON.parse(payloadStr);

                // Store role in local storage as backend returns it in standard signin
                localStorage.setItem('user_role', payload.role);
                if (payload.sub) {
                    localStorage.setItem('user_email', payload.sub);
                }

                // Navigate based on role
                if (payload.role === 'ADMIN') {
                    this.router.navigate(['/admin/dashboard']);
                } else if (payload.role === 'PROJECT_OWNER') {
                    this.router.navigate(['/client']);
                } else {
                    this.router.navigate(['/projects']);
                }
            } else {
                this.router.navigate(['/login']);
            }
        });
    }
}
