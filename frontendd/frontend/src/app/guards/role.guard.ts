import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Accès réservé aux utilisateurs avec le rôle ADMIN (backoffice). */
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }
  if (auth.isAdmin()) return true;
  if (auth.isFreelancer()) {
    router.navigate(['/freelancer/skills']);
    return false;
  }
  if (auth.isProjectOwner()) {
    router.navigate(['/project-owner/projects']);
    return false;
  }
  router.navigate(['/']);
  return false;
};

export const freelancerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }
  if (auth.isFreelancer()) return true;
  if (auth.isProjectOwner()) {
    router.navigate(['/project-owner/projects']);
    return false;
  }
  router.navigate(['/']);
  return false;
};

export const projectOwnerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }
  if (auth.isProjectOwner()) return true;
  if (auth.isFreelancer()) {
    router.navigate(['/freelancer/skills']);
    return false;
  }
  router.navigate(['/']);
  return false;
};
