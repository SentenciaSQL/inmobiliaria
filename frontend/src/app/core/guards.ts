import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from './session.service';

export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);
  return session.user() ? true : router.createUrlTree(['/ingresar']);
};

export const companyGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);
  if (!session.user()) return router.createUrlTree(['/ingresar']);
  return session.company() ? true : router.createUrlTree(['/seleccionar-empresa']);
};

export const roleGuard = (roles: string[]): CanActivateFn => () => {
  const session = inject(SessionService);
  const router = inject(Router);
  const role = session.company()?.role;
  return role && roles.includes(role) ? true : router.createUrlTree(['/seleccionar-empresa']);
};

export const platformGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);
  return session.user()?.platformAdmin ? true : router.createUrlTree(['/']);
};
