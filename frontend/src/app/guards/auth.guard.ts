import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Toute route front nécessitant une session (ex. classement & matching). */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  return true;
};

/**
 * Garde pour les routes /admin : exige une connexion et le rôle ADMIN.
 * Sinon redirection vers login (avec returnUrl) ou accueil.
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url },
    });
  }

  if (auth.getStoredUser()?.role !== 'ADMIN') {
    return router.createUrlTree(['/']);
  }

  return true;
};

/** Routes réservées au porteur de projet (PROJECT_OWNER mappé en CLIENT côté front). */
export const clientGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
  const r = auth.getStoredUser()?.role;
  if (r !== 'CLIENT' && r !== 'PROJECT_OWNER') {
    return router.createUrlTree(['/']);
  }
  return true;
};
