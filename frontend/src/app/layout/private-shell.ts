import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ROLE_LABELS, label } from '../core/labels';
import { Brand } from '../core/models';
import { SessionService } from '../core/session.service';
import { Logo } from '../shared/ui';

interface NavItem { path: string; label: string; roles: string[]; }

@Component({
  selector: 'app-private-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Logo],
  template: `
    <div class="min-h-screen md:grid md:grid-cols-[240px_1fr]">
      <aside class="no-print border-b border-line bg-ink text-sand md:min-h-screen md:border-b-0 md:border-r">
        <div class="flex items-center justify-between gap-3 px-4 py-4">
          <a routerLink="/" class="flex items-center gap-2">
            <span class="h-9 w-9 overflow-hidden rounded-lg bg-white"><app-logo [src]="brand()?.logoUrl ?? null" [name]="brand()?.commercialName || 'Alzada'" [alt]="brand()?.commercialName || 'Alzada'" /></span>
            <span class="font-display text-lg leading-tight">{{ brand()?.commercialName || 'Alzada' }}</span>
          </a>
          <button type="button" class="rounded-full border border-sand/30 px-3 py-1 text-xs md:hidden" (click)="open.set(!open())">Menú</button>
        </div>
        <nav class="px-3 pb-4" [class.hidden]="!open()" [class.md:block]="true">
          @for (item of visible(); track item.path) {
            <a class="mb-1 block rounded-xl px-3 py-2 text-sm text-sand/80 hover:bg-white/10" routerLinkActive="bg-white/15 text-white" [routerLink]="item.path">{{ item.label }}</a>
          }
        </nav>
      </aside>
      <div>
        <header class="no-print flex flex-wrap items-center justify-between gap-3 border-b border-line bg-paper px-4 py-3">
          <p class="text-sm text-ink-soft">{{ role() }} · {{ session.user()?.fullName }}</p>
          <div class="flex gap-3 text-sm">
            @if ((session.user()?.memberships?.length ?? 0) > 1) {
              <a routerLink="/seleccionar-empresa">Cambiar empresa</a>
            }
            <a routerLink="/app/notificaciones">Avisos @if (unread()) { ({{ unread() }}) }</a>
            <a routerLink="/cuenta">Contraseña</a>
            <button type="button" (click)="session.logout()">Salir</button>
          </div>
        </header>
        <div class="px-4 py-6"><router-outlet /></div>
      </div>
    </div>
  `,
})
export class PrivateShell implements OnInit {
  readonly session = inject(SessionService);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly brand = signal<Brand | null>(null);
  readonly unread = signal(0);
  readonly open = signal(false);
  private readonly items: NavItem[] = [
    { path: '/app', label: 'Resumen', roles: ['COMPANY_ADMIN', 'SALES_ADVISOR', 'FINANCE'] },
    { path: '/app/empresa', label: 'Empresa', roles: ['COMPANY_ADMIN'] },
    { path: '/app/proyectos', label: 'Proyectos', roles: ['COMPANY_ADMIN', 'SALES_ADVISOR', 'FINANCE'] },
    { path: '/app/edificios', label: 'Edificios', roles: ['COMPANY_ADMIN', 'SALES_ADVISOR', 'FINANCE'] },
    { path: '/app/unidades', label: 'Unidades', roles: ['COMPANY_ADMIN', 'SALES_ADVISOR', 'FINANCE'] },
    { path: '/app/prospectos', label: 'Prospectos', roles: ['COMPANY_ADMIN', 'SALES_ADVISOR'] },
    { path: '/app/reservas', label: 'Reservas', roles: ['COMPANY_ADMIN', 'SALES_ADVISOR'] },
    { path: '/app/compradores', label: 'Compradores', roles: ['COMPANY_ADMIN', 'SALES_ADVISOR', 'FINANCE'] },
    { path: '/app/contratos', label: 'Contratos', roles: ['COMPANY_ADMIN', 'SALES_ADVISOR', 'FINANCE'] },
    { path: '/app/pagos', label: 'Pagos', roles: ['COMPANY_ADMIN', 'FINANCE'] },
    { path: '/app/equipo', label: 'Equipo', roles: ['COMPANY_ADMIN'] },
    { path: '/app/plantillas', label: 'Plantillas', roles: ['COMPANY_ADMIN'] },
    { path: '/app/auditoria', label: 'Auditoría', roles: ['COMPANY_ADMIN'] },
  ];

  ngOnInit(): void {
    this.api.brand().subscribe({ next: (brand) => this.brand.set(brand) });
    this.api.unread().subscribe({ next: (result) => this.unread.set(result.count) });
  }

  visible(): NavItem[] {
    const role = this.session.company()?.role ?? '';
    return this.items.filter((item) => item.roles.includes(role));
  }

  role(): string {
    return label(ROLE_LABELS, this.session.company()?.role);
  }
}

@Component({
  selector: 'app-platform-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen md:grid md:grid-cols-[220px_1fr]">
      <aside class="bg-ink text-sand md:min-h-screen">
        <p class="px-4 py-5 font-display text-2xl">Alzada</p>
        <nav class="px-3">
          <a class="mb-1 block rounded-xl px-3 py-2 text-sm" routerLink="/plataforma" routerLinkActive="bg-white/15">Métricas</a>
          <a class="mb-1 block rounded-xl px-3 py-2 text-sm" routerLink="/plataforma/constructoras" routerLinkActive="bg-white/15">Constructoras</a>
          <a class="mb-1 block rounded-xl px-3 py-2 text-sm" routerLink="/plataforma/configuracion" routerLinkActive="bg-white/15">Configuración</a>
        </nav>
      </aside>
      <div>
        <header class="flex justify-end gap-4 border-b border-line bg-paper px-4 py-3 text-sm">
          <a routerLink="/cuenta">Contraseña</a>
          <button type="button" (click)="session.logout()">Salir</button>
        </header>
        <div class="px-4 py-6"><router-outlet /></div>
      </div>
    </div>
  `,
})
export class PlatformShell {
  readonly session = inject(SessionService);
  private readonly router = inject(Router);
}
