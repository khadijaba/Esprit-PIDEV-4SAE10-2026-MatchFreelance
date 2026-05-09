import { HttpInterceptorFn } from '@angular/common/http';
import { apiBaseUrl } from '../../environments/environment';

/**
 * En prod (Vercel, etc.), les rewrites peuvent encore renvoyer 405 sur POST /api.
 * On envoie alors les requêtes /api directement vers la Gateway Render (CORS côté Gateway).
 */
export const apiOriginInterceptor: HttpInterceptorFn = (req, next) => {
  const base = apiBaseUrl();
  if (base && req.url.startsWith('/api')) {
    req = req.clone({ url: base + req.url });
  }
  return next(req);
};
