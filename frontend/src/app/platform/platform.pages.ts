import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuditEvent, CompanyProfile, PlatformMetrics, Province, TemplateView } from '../core/models';
import { EmptyState, Loading, Pager, problemMessage, when } from '../shared/ui';

@Component({
  selector: 'app-platform-home',
  imports: [Loading],
  template: `
    <h1 class="text-4xl">Plataforma</h1>
    @if (loading()) { <app-loading /> }
    @else if (metrics(); as data) {
      <div class="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <article class="rounded-2xl bg-paper p-4"><p class="text-sm text-ink-soft">Constructoras activas</p><p class="font-display text-3xl">{{ data.activeCompanies }}</p></article>
        <article class="rounded-2xl bg-paper p-4"><p class="text-sm text-ink-soft">Suspendidas</p><p class="font-display text-3xl">{{ data.suspendedCompanies }}</p></article>
        <article class="rounded-2xl bg-paper p-4"><p class="text-sm text-ink-soft">Proyectos publicados</p><p class="font-display text-3xl">{{ data.publishedProjects }}</p></article>
        <article class="rounded-2xl bg-paper p-4"><p class="text-sm text-ink-soft">Usuarios</p><p class="font-display text-3xl">{{ data.users }}</p></article>
      </div>
    }
  `,
})
export class PlatformHomePage implements OnInit {
  private readonly api = inject(ApiService);
  readonly metrics = signal<PlatformMetrics | null>(null);
  readonly loading = signal(true);
  ngOnInit(): void { this.api.platformMetrics().subscribe({ next: (metrics) => { this.metrics.set(metrics); this.loading.set(false); }, error: () => this.loading.set(false) }); }
}

@Component({
  selector: 'app-platform-companies',
  imports: [ReactiveFormsModule, Loading, EmptyState, Pager],
  template: `
    <h1 class="text-4xl">Constructoras</h1>
    <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-4 md:grid-cols-2" [formGroup]="form" (ngSubmit)="create()">
      <label class="text-sm">Nombre comercial<input class="field" formControlName="commercialName" /></label>
      <label class="text-sm">Razón social<input class="field" formControlName="legalName" /></label>
      <label class="text-sm">RNC<input class="field" formControlName="rnc" /></label>
      <label class="text-sm">Provincia<select class="field" formControlName="provinceCode"><option value="">Seleccione</option>@for (item of provinces(); track item.code) { <option [value]="item.code">{{ item.name }}</option> }</select></label>
      <label class="text-sm">Administrador<input class="field" formControlName="adminFullName" /></label>
      <label class="text-sm">Correo del administrador<input class="field" formControlName="adminEmail" /></label>
      <label class="text-sm md:col-span-2">Contraseña inicial<input class="field" type="password" formControlName="adminPassword" /></label>
      <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Registrar constructora</button>
    </form>
    @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin constructoras" detail="Registre la primera empresa para que administre sus proyectos." /></div> }
    @else {
      <ul class="mt-4 space-y-2">@for (item of items(); track item.id) {
        <li class="flex flex-wrap items-center justify-between gap-2 rounded-2xl bg-paper p-3">
          <span>{{ item.commercialName }} · {{ item.status === 'ACTIVE' ? 'Activa' : 'Suspendida' }}</span>
          <button type="button" (click)="toggle(item)">{{ item.status === 'ACTIVE' ? 'Suspender' : 'Activar' }}</button>
        </li>
      }</ul>
      <app-pager [page]="page()" [pages]="pages()" [total]="total()" (pageChange)="load($event)" />
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class PlatformCompaniesPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  readonly items = signal<CompanyProfile[]>([]);
  readonly provinces = signal<Province[]>([]);
  readonly loading = signal(true);
  readonly message = signal('');
  readonly page = signal(0); readonly pages = signal(0); readonly total = signal(0);
  readonly form = this.formBuilder.nonNullable.group({
    commercialName: ['', Validators.required], legalName: ['', Validators.required], rnc: [''], provinceCode: [''], municipalityId: [''],
    phone: [''], email: [''], address: [''], website: [''], description: [''], publicPhone: [''], publicEmail: [''], publicContact: [''],
    adminFullName: ['', Validators.required], adminEmail: ['', [Validators.required, Validators.email]], adminPhone: [''], adminPassword: ['', Validators.required],
  });
  ngOnInit(): void { this.api.locations().subscribe((items) => this.provinces.set(items)); this.load(0); }
  load(page: number): void { this.api.platformCompanies({ page, size: 20 }).subscribe({ next: (result) => { this.items.set(result.content); this.page.set(result.page); this.pages.set(result.totalPages); this.total.set(result.totalElements); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  create(): void {
    this.api.createCompany(this.form.getRawValue()).subscribe({ next: () => { this.message.set('Constructora registrada. Su administrador ya puede ingresar y subir el logo.'); this.load(0); }, error: (error) => this.message.set(problemMessage(error)) });
  }
  toggle(item: CompanyProfile): void {
    const status = item.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    if (status === 'SUSPENDED' && !confirm('¿Suspender esta constructora? Sus usuarios no podrán operar.')) return;
    this.api.companyStatus(item.id, status).subscribe({ next: () => this.load(this.page()), error: (error) => this.message.set(problemMessage(error)) });
  }
}

@Component({
  selector: 'app-platform-settings',
  imports: [ReactiveFormsModule],
  template: `
    <h1 class="text-4xl">Configuración</h1>
    @if (!loading()) {
      <form class="mt-4 grid max-w-xl gap-3" [formGroup]="form" (ngSubmit)="save()">
        <label class="text-sm">Nombre<input class="field" formControlName="platformName" /></label>
        <label class="text-sm">Correo de soporte<input class="field" formControlName="supportEmail" /></label>
        <label class="text-sm">Texto del marketplace<textarea class="field" rows="3" formControlName="tagline"></textarea></label>
        <label class="text-sm">Moneda predeterminada<input class="field" formControlName="currency" maxlength="3" /></label>
        <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Guardar</button>
      </form>
      @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
      <h2 class="mt-8 text-2xl">Plantillas globales</h2>
      <div class="mt-3 space-y-3">@for (item of templates(); track item.code) {
        <form class="rounded-2xl bg-paper p-4" (submit)="saveTemplate(item, $event)">
          <p class="text-sm">{{ item.description || item.code }}</p>
          <input class="field mt-2" name="subject" [value]="item.subject" />
          <textarea class="field mt-2" name="body" rows="3">{{ item.body }}</textarea>
          <button class="mt-2 text-sm" type="submit">Actualizar</button>
        </form>
      }</div>
      <h2 class="mt-8 text-2xl">Auditoría de plataforma</h2>
      <ul class="mt-3 space-y-2 text-sm">@for (item of audit(); track item.id) { <li>{{ when(item.createdAt, true) }} · {{ item.summary }}</li> }</ul>
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class PlatformSettingsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  readonly loading = signal(true);
  readonly message = signal('');
  readonly templates = signal<TemplateView[]>([]);
  readonly audit = signal<AuditEvent[]>([]);
  readonly form = this.formBuilder.nonNullable.group({ platformName: ['', Validators.required], supportEmail: [''], tagline: ['', Validators.required], currency: ['DOP'] });
  ngOnInit(): void {
    this.api.platformSettings().subscribe((settings) => {
      this.form.patchValue({ platformName: settings['platform_name'] ?? '', supportEmail: settings['support_email'] ?? '', tagline: settings['marketplace_tagline'] ?? '', currency: settings['default_currency'] ?? 'DOP' });
      this.loading.set(false);
    });
    this.api.platformTemplates().subscribe((templates) => this.templates.set(templates));
    this.api.platformAudit().subscribe((page) => this.audit.set(page.content));
  }
  save(): void { this.api.updatePlatformSettings(this.form.getRawValue()).subscribe({ next: () => this.message.set('Configuración guardada.'), error: (error) => this.message.set(problemMessage(error)) }); }
  saveTemplate(item: TemplateView, event: Event): void {
    event.preventDefault();
    const data = new FormData(event.target as HTMLFormElement);
    this.api.updatePlatformTemplate(item.code, String(data.get('subject') ?? ''), String(data.get('body') ?? '')).subscribe({ next: () => this.message.set('Plantilla global actualizada.'), error: (error) => this.message.set(problemMessage(error)) });
  }
  readonly when = when;
}
