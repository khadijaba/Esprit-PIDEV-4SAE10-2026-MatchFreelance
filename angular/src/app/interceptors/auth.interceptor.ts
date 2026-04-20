import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAuthHeader();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: token } });
  }
  return next(req);
};
