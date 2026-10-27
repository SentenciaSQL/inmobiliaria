import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ROLE_LABELS, label } from '../core/labels';
import { SessionService } from '../core/session.service';
import { Logo, problemMessage } from '../shared/ui';

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
        @if (error()) { <p class="mt-3 text-sm text-danger" role="alert">{{ error() }}</p> }
        <button class="mt-5 w-full rounded-full bg-sea px-4 py-2 text-sand disabled:opacity-50" [disabled]="form.invalid || loading()" type="submit">
          {{ loading() ? 'Ingresando…' : 'Entrar' }}
        </button>
      </form>
    </main>
  `,
})
export class LoginPage {
  private readonly session = inject(SessionService);
  private readonly formBuilder = inject(FormBuilder);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

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
  selector: 'app-company-switch',
  imports: [Logo],
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
    </main>
  `,
})
export class CompanySwitchPage {
  readonly session = inject(SessionService);
  role(value: string) { return label(ROLE_LABELS, value); }
}
