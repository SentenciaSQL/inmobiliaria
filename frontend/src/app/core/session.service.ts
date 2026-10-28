import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, of, tap, throwError } from 'rxjs';
import { ApiService } from './api.service';
import { Membership, Session, User } from './models';

const COMPANY_KEY = 'alzada.company';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  readonly user = signal<User | null>(null);
  readonly token = signal<string | null>(null);
  readonly company = signal<Membership | null>(this.readCompany());
  readonly ready = signal(false);

  readonly isStaff = computed(() => {
    const role = this.company()?.role;
    return role === 'COMPANY_ADMIN' || role === 'SALES_ADVISOR' || role === 'FINANCE';
  });

  login(email: string, password: string): Observable<Session> {
    return this.api.login(email, password).pipe(tap((session) => this.apply(session)));
  }

  refresh(): Observable<Session> {
    return this.api.refresh().pipe(
      tap((session) => this.apply(session, false)),
      catchError((error) => {
        this.clear();
        return throwError(() => error);
      }),
    );
  }

  restore(): Observable<Session | null> {
    return this.refresh().pipe(
      catchError(() => {
        this.ready.set(true);
        return of(null);
      }),
      tap(() => this.ready.set(true)),
    );
  }

  logout(): void {
    this.api.logout().subscribe({ complete: () => this.finishLogout(), error: () => this.finishLogout() });
  }

  select(membership: Membership): void {
    this.company.set(membership);
    sessionStorage.setItem(COMPANY_KEY, JSON.stringify(membership));
    void this.router.navigateByUrl(this.homeFor(membership.role));
  }

  homeFor(role: string): string {
    if (role === 'BUYER') return '/portal';
    return '/app';
  }

  private apply(session: Session, route = true): void {
    this.token.set(session.accessToken);
    this.user.set(session.user);
    const stored = this.company();
    const match = stored ? session.user.memberships.find((item) => item.companyId === stored.companyId && item.role === stored.role) : null;
    if (match) {
      this.company.set(match);
      sessionStorage.setItem(COMPANY_KEY, JSON.stringify(match));
    } else if (session.user.memberships.length === 1) {
      this.select(session.user.memberships[0]);
      return;
    } else {
      this.company.set(null);
      sessionStorage.removeItem(COMPANY_KEY);
    }
    if (route) {
      if (this.company()) {
        void this.router.navigateByUrl(this.homeFor(this.company()!.role));
      } else if (session.user.platformAdmin && session.user.memberships.length === 0) {
        void this.router.navigateByUrl('/plataforma');
      } else {
        void this.router.navigateByUrl('/seleccionar-empresa');
      }
    }
  }

  private finishLogout(): void {
    this.clear();
    void this.router.navigateByUrl('/');
  }

  private clear(): void {
    this.token.set(null);
    this.user.set(null);
    this.company.set(null);
    sessionStorage.removeItem(COMPANY_KEY);
  }

  private readCompany(): Membership | null {
    const raw = sessionStorage.getItem(COMPANY_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as Membership;
    } catch {
      return null;
    }
  }
}
