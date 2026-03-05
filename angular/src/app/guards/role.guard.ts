import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/user.model';

function redirectByRole(router: Router, role: UserRole | undefined) {
  if (!role) {
    router.navigate(['/login']);
    return;
  }
  if (role === 'ADMIN') {
    router.navigate(['/admin']);
  } else if (role === 'CLIENT') {
    router.navigate(['/client']);
  } else {
    router.navigate(['/freelancer']);
  }
}

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUser();

  if (user && user.role === 'ADMIN') {
    return true;
  }

  if (!user) {
    router.navigate(['/login']);
  } else {
    redirectByRole(router, user.role);
  }
  return false;
};

export const clientGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUser();

  if (user && user.role === 'CLIENT') {
    return true;
  }

  if (!user) {
    router.navigate(['/login']);
  } else {
    redirectByRole(router, user.role);
  }
  return false;
};

export const freelancerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUser();

  if (user && user.role === 'FREELANCER') {
    return true;
  }

  if (!user) {
    router.navigate(['/login']);
  } else {
    redirectByRole(router, user.role);
  }
  return false;
};

