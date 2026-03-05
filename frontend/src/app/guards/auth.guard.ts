import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

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
