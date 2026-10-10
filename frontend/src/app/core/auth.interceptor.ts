import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { API_BASE_URL } from './api.config';
import { AuthService } from './auth.service';

/** Adds the bearer token to backend calls and sends the user back to login when the token is rejected. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(`${API_BASE_URL}/api/`)) {
    return next(req);
  }

  const auth = inject(AuthService);
  const router = inject(Router);
  const isLogin = req.url === `${API_BASE_URL}/api/auth/login`;
  const isAuthCall = req.url.startsWith(`${API_BASE_URL}/api/auth/`);
  const token = auth.session()?.token;
  const request = token && !isLogin ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(request).pipe(
    catchError((err: unknown) => {
      // Login and logout handle their own 401s; anywhere else it means the token expired or was revoked
      if (err instanceof HttpErrorResponse && err.status === 401 && !isAuthCall) {
        auth.clearSession();
        void router.navigate(['/login'], { queryParams: { reason: 'expired' } });
      }
      return throwError(() => err);
    }),
  );
};
