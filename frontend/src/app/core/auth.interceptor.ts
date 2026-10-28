import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { SessionService } from './session.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(SessionService);
  const router = inject(Router);
  const authed = withAuth(request, session);
  return next(authed).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && session.token() && !request.url.includes('/api/v1/auth/')) {
        return session.refresh().pipe(
          switchMap(() => next(withAuth(request, session))),
          catchError((refreshError) => {
            void router.navigateByUrl('/ingresar');
            return throwError(() => refreshError);
          }),
        );
      }
      if (error.status === 409 && error.error?.code === 'COMPANY_REQUIRED') {
        void router.navigateByUrl('/seleccionar-empresa');
      }
      return throwError(() => error);
    }),
  );
};

function withAuth(request: Parameters<HttpInterceptorFn>[0], session: SessionService) {
  const headers: Record<string, string> = {};
  const token = session.token();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const company = session.company();
  const tenantScoped = !request.url.includes('/api/v1/public') && !request.url.includes('/api/v1/platform') && !request.url.includes('/api/v1/auth');
  if (company && tenantScoped) {
    headers['X-Company-Id'] = company.companyId;
    headers['X-Company-Role'] = company.role;
  }
  return request.clone({ setHeaders: headers, withCredentials: true });
}
