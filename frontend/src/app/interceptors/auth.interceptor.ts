import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Angular peut fournir une URL absolue (http://localhost:4200/api/...), pas seulement /api/... */
function isApiPath(url: string): boolean {
  if (url.startsWith('/api/')) {
    return true;
  }
  try {
    const path = url.startsWith('http') ? new URL(url).pathname : url;
    return path.startsWith('/api/');
  } catch {
    return false;
  }
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!isApiPath(req.url)) {
    return next(req);
  }
  return from(auth.ensureFreshTokenIfNeeded()).pipe(
    switchMap(() => {
      const token = auth.getToken();
      if (token) {
        req = req.clone({
          setHeaders: { Authorization: `Bearer ${token}` },
        });
      }
      return next(req);
    })
  );
};
