import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/auth.model';

export const roleGuard = (allowedRoles: UserRole[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }
  const user = auth.user();
  if (user && allowedRoles.includes(user.role)) return true;
  const fallback = user?.role === 'ADMIN' ? '/admin' : user?.role === 'CLIENT' ? '/client' : '/';
  router.navigate([fallback]);
  return false;
};
