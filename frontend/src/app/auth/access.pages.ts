import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ROLE_LABELS, label } from '../core/labels';
import { SessionService } from '../core/session.service';
import { Logo, problemMessage } from '../shared/ui';

function meetsPasswordPolicy(value: string): boolean {
  return value.length >= 10 && value.length <= 80 && /\p{L}/u.test(value) && /\d/.test(value);
}

function passwordPolicy(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '');
  if (!value || meetsPasswordPolicy(value)) return null;
  return { policy: true };
}

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Logo],
  template: `
    <main class="mx-auto grid min-h-screen max-w-5xl items-center gap-10 px-4 py-10 md:grid-cols-2">
      <div>
        <a routerLink="/" class="inline-flex items-center gap-3">
          <span class="h-12 w-12 overflow-hidden rounded-2xl"><app-logo name="Alzada" alt="Alzada" /></span>
          <span class="font-display text-3xl">Alzada</span>
        </a>
        <p class="mt-6 max-w-md text-ink-soft">Acceso para constructoras, asesores, finanzas y compradores. Cada empresa solo ve su propia información.</p>
      </div>
      <form class="rounded-3xl border border-line bg-paper p-6 shadow-sm" [formGroup]="form" (ngSubmit)="submit()">
        <h1 class="text-3xl">Ingresar</h1>
        <label class="mt-4 block text-sm">Correo
          <input class="mt-1 w-full rounded-xl border border-line px-3 py-2" type="email" formControlName="email" autocomplete="username" />
        </label>
        <label class="mt-3 block text-sm">Contraseña
          <input class="mt-1 w-full rounded-xl border border-line px-3 py-2" type="password" formControlName="password" autocomplete="current-password" />
        </label>
        @if (notice()) { <p class="mt-3 text-sm text-sea" role="status">{{ notice() }}</p> }
        @if (error()) { <p class="mt-3 text-sm text-danger" role="alert">{{ error() }}</p> }
        <button class="mt-5 w-full rounded-full bg-sea px-4 py-2 text-sand disabled:opacity-50" [disabled]="form.invalid || loading()" type="submit">
          {{ loading() ? 'Ingresando…' : 'Entrar' }}
        </button>
      </form>
    </main>
  `,
})
export class LoginPage implements OnInit {
  private readonly session = inject(SessionService);
  private readonly formBuilder = inject(FormBuilder);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  ngOnInit(): void {
    const notice = history.state?.notice;
    if (typeof notice === 'string' && notice.trim()) this.notice.set(notice);
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set('');
    const value = this.form.getRawValue();
    this.session.login(value.email, value.password).subscribe({
      error: (error) => { this.loading.set(false); this.error.set(problemMessage(error)); },
    });
  }
}

@Component({
  selector: 'app-password',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="mx-auto max-w-lg px-4 py-12">
      <a class="text-sm text-sea" [routerLink]="back()">Volver</a>
      <form class="mt-4 rounded-3xl border border-line bg-paper p-6" [formGroup]="form" (ngSubmit)="submit()">
        <h1 class="text-3xl">Cambiar contraseña</h1>
        <p class="mt-2 text-sm text-ink-soft">Mínimo 10 caracteres, con una letra y un número. Al guardarla, esta sesión se cierra y deberá ingresar de nuevo. Las otras sesiones también quedan cerradas.</p>
        <label class="mt-4 block text-sm">Contraseña actual
          <input class="mt-1 w-full rounded-xl border border-line px-3 py-2" type="password" formControlName="currentPassword" autocomplete="current-password" />
        </label>
        <label class="mt-3 block text-sm">Nueva contraseña
          <input class="mt-1 w-full rounded-xl border border-line px-3 py-2" type="password" formControlName="newPassword" autocomplete="new-password" />
        </label>
        @if (form.controls.newPassword.touched && form.controls.newPassword.hasError('policy')) {
          <p class="mt-1 text-sm text-danger">La contraseña debe tener entre 10 y 80 caracteres, una letra y un número.</p>
        }
        <label class="mt-3 block text-sm">Confirmar nueva contraseña
          <input class="mt-1 w-full rounded-xl border border-line px-3 py-2" type="password" formControlName="confirmPassword" autocomplete="new-password" />
        </label>
        @if (form.controls.confirmPassword.touched && form.hasError('mismatch')) {
          <p class="mt-1 text-sm text-danger">Las contraseñas nuevas no coinciden.</p>
        }
        @if (error()) { <p class="mt-3 text-sm text-danger" role="alert">{{ error() }}</p> }
        <button class="mt-5 rounded-full bg-sea px-4 py-2 text-sand disabled:opacity-50" [disabled]="form.invalid || loading()" type="submit">
          {{ loading() ? 'Guardando…' : 'Guardar contraseña' }}
        </button>
      </form>
    </main>
  `,
})
export class PasswordPage {
  private readonly session = inject(SessionService);
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = this.formBuilder.nonNullable.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, passwordPolicy]],
    confirmPassword: ['', Validators.required],
  }, { validators: passwordsMatch });

  back(): string {
    const role = this.session.company()?.role;
    if (role === 'BUYER') return '/portal';
    if (role) return '/app';
    if (this.session.user()?.platformAdmin) return '/plataforma';
    return '/seleccionar-empresa';
  }

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    const value = this.form.getRawValue();
    if (value.newPassword === value.currentPassword) {
      this.error.set('La nueva contraseña debe ser distinta de la actual.');
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.api.changePassword(value.currentPassword, value.newPassword).subscribe({
      next: () => {
        this.session.discard();
        void this.router.navigateByUrl('/ingresar', { state: { notice: 'Contraseña actualizada. Ingrese con la nueva.' } });
      },
      error: (error) => {
        this.loading.set(false);
        this.error.set(passwordProblem(error));
      },
    });
  }
}

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const next = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  if (!next || !confirm || next === confirm) return null;
  return { mismatch: true };
}

function passwordProblem(error: unknown): string {
  const body = errorBody(error);
  if (body?.errors && typeof body.errors === 'object') {
    const message = Object.values(body.errors).find((item) => typeof item === 'string' && item.trim());
    if (typeof message === 'string') return message;
  }
  if (typeof body?.detail === 'string' && body.detail.trim()) return body.detail;
  return problemMessage(error);
}

function errorBody(error: unknown): { detail?: string; errors?: Record<string, unknown> } | null {
  if (!(error instanceof HttpErrorResponse)) return null;
  const payload = error.error;
  if (payload && typeof payload === 'object') return payload as { detail?: string; errors?: Record<string, unknown> };
  if (typeof payload === 'string' && payload.trim().startsWith('{')) {
    try {
      return JSON.parse(payload) as { detail?: string; errors?: Record<string, unknown> };
    } catch {
      return null;
    }
  }
  return null;
}

@Component({
  selector: 'app-company-switch',
  imports: [Logo, RouterLink],
  template: `
    <main class="mx-auto max-w-3xl px-4 py-12">
      <h1 class="text-4xl">Seleccione la constructora</h1>
      <p class="mt-2 text-sm text-ink-soft">Su acceso queda limitado a la empresa y al rol que elija.</p>
      <div class="mt-6 grid gap-3">
        @for (membership of session.user()?.memberships ?? []; track membership.id) {
          <button type="button" class="flex items-center gap-4 rounded-2xl border border-line bg-paper p-4 text-left" (click)="session.select(membership)">
            <span class="h-12 w-12 overflow-hidden rounded-xl"><app-logo [src]="membership.logoUrl" [name]="membership.companyName" [alt]="membership.companyName" /></span>
            <span>
              <span class="block font-display text-2xl">{{ membership.companyName }}</span>
              <span class="text-sm text-ink-soft">{{ role(membership.role) }}</span>
            </span>
          </button>
        }
        @if (session.user()?.platformAdmin) {
          <a class="text-sm text-sea" href="/plataforma">Ir a la administración de la plataforma</a>
        }
      </div>
      <a class="mt-6 inline-block text-sm text-sea" routerLink="/cuenta">Cambiar contraseña</a>
    </main>
  `,
})
export class CompanySwitchPage {
  readonly session = inject(SessionService);
  role(value: string) { return label(ROLE_LABELS, value); }
}
