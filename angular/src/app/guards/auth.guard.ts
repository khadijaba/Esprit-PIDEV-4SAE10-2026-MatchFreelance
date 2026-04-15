import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isLoggedIn()) {
        router.navigate(['/login']);
        return false;
    }

    const expectedRole = route.data['role'];
    if (expectedRole) {
        const userRole = authService.getUserRole();
        if (userRole !== expectedRole) {
            router.navigate(['/']); // Or forbidden page
            return false;
        }
    }

    return true;
};
