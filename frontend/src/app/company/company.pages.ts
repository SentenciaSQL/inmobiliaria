import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import {
  AVAILABILITY, CONTRACT_STATUS, INSTALLMENT_STATUS, PAYMENT_STATUS, PROJECT_STATUS, PROJECT_TYPES, PROPERTY_TYPES, PUBLICATION, RESERVATION_STATUS, ROLE_LABELS, label,
} from '../core/labels';
import {
  AuditEvent, CompanyProfile, Contract, Customer, Dashboard, DocumentItem, Lead, Member, Notice, Payment, PlanView, ProjectDetail, Province, Reservation, Stage, TemplateView, Unit,
} from '../core/models';
import { EmptyState, Loading, Logo, Pager, money, problemMessage, when } from '../shared/ui';

@Component({
  selector: 'app-dashboard',
  imports: [Loading],
  template: `
    <div class="flex flex-wrap items-end justify-between gap-3">
      <h1 class="text-4xl">Resumen</h1>
      <form class="flex flex-wrap gap-2 text-sm" (ngSubmit)="load()">
        <label>Desde <input class="rounded-lg border border-line px-2 py-1" type="date" [value]="from()" (input)="from.set(inputValue($event))" /></label>
        <label>Hasta <input class="rounded-lg border border-line px-2 py-1" type="date" [value]="to()" (input)="to.set(inputValue($event))" /></label>
        <button class="rounded-full bg-sea px-3 py-1 text-sand" type="submit">Actualizar</button>
      </form>
    </div>
    @if (loading()) { <app-loading /> }
    @else if (data(); as metrics) {
      <div class="mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        @for (item of cards(metrics); track item.label) {
          <article class="rounded-2xl border border-line bg-paper p-4">
            <p class="text-sm text-ink-soft">{{ item.label }}</p>
            <p class="mt-2 font-display text-3xl">{{ item.value }}</p>
          </article>
        }
      </div>
    }
  `,
})
export class DashboardPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly data = signal<Dashboard | null>(null);
  readonly loading = signal(true);
  readonly from = signal('');
  readonly to = signal('');

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.api.dashboard(this.from() || undefined, this.to() || undefined).subscribe({
      next: (data) => { this.data.set(data); this.from.set(data.from); this.to.set(data.to); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  cards(metrics: Dashboard) {
    return [
      { label: 'Proyectos', value: String(metrics.projects) },
      { label: 'Unidades disponibles', value: String(metrics.availableUnits) },
      { label: 'Reservadas', value: String(metrics.reservedUnits) },
      { label: 'Vendidas', value: String(metrics.soldUnits) },
      { label: 'Prospectos abiertos', value: String(metrics.openLeads) },
      { label: 'Reservas por vencer', value: String(metrics.reservationsExpiringSoon) },
      { label: 'Cuotas vencidas', value: String(metrics.overdueInstallments) },
      { label: 'Cobros del período', value: money(metrics.collections, metrics.currency) },
    ];
  }

  inputValue(event: Event): string { return (event.target as HTMLInputElement).value; }
}

@Component({
  selector: 'app-company-profile',
  imports: [ReactiveFormsModule, Logo],
  template: `
    <h1 class="text-4xl">Empresa</h1>
    @if (profile(); as company) {
      <div class="mt-6 grid gap-6 lg:grid-cols-[220px_1fr]">
        <section class="rounded-3xl border border-line bg-paper p-4">
          <div class="mx-auto h-36 w-36 overflow-hidden rounded-3xl border border-line bg-white">
            <app-logo [src]="preview() || company.logoUrl" [name]="company.commercialName" [alt]="company.commercialName" />
          </div>
          <label class="mt-4 block text-sm">Seleccionar logo
            <input class="mt-1 block w-full text-sm" type="file" accept="image/png,image/jpeg,image/webp" (change)="choose($event)" />
          </label>
          <p class="mt-2 text-xs text-ink-soft">PNG, JPEG o WebP. Máximo 2 MB. El SVG no se acepta.</p>
          @if (logoMessage()) { <p class="mt-2 text-sm" role="status">{{ logoMessage() }}</p> }
          <div class="mt-3 flex gap-2">
            <button type="button" class="rounded-full bg-sea px-3 py-1 text-sm text-sand disabled:opacity-50" [disabled]="!file() || uploading()" (click)="upload()">{{ uploading() ? 'Guardando…' : 'Guardar logo' }}</button>
            <button type="button" class="rounded-full border border-line px-3 py-1 text-sm" (click)="remove()">Quitar logo</button>
          </div>
        </section>
        <form class="grid gap-3 rounded-3xl border border-line bg-paper p-5 md:grid-cols-2" [formGroup]="form" (ngSubmit)="save()">
          <label class="text-sm">Nombre comercial<input class="field" formControlName="commercialName" /></label>
          <label class="text-sm">Razón social<input class="field" formControlName="legalName" /></label>
          <label class="text-sm">RNC<input class="field" formControlName="rnc" /></label>
          <label class="text-sm">Teléfono<input class="field" formControlName="phone" /></label>
          <label class="text-sm">Correo<input class="field" type="email" formControlName="email" /></label>
          <label class="text-sm">Sitio web<input class="field" formControlName="website" /></label>
          <label class="text-sm md:col-span-2">Dirección<input class="field" formControlName="address" /></label>
          <label class="text-sm">Provincia
            <select class="field" formControlName="provinceCode"><option value="">Seleccione</option>@for (item of provinces(); track item.code) { <option [value]="item.code">{{ item.name }}</option> }</select>
          </label>
          <label class="text-sm">Municipio
            <select class="field" formControlName="municipalityId"><option value="">Seleccione</option>@for (item of municipalities(); track item.id) { <option [value]="item.id">{{ item.name }}</option> }</select>
          </label>
          <label class="text-sm md:col-span-2">Descripción<textarea class="field" rows="4" formControlName="description"></textarea></label>
          <label class="text-sm">Teléfono público<input class="field" formControlName="publicPhone" /></label>
          <label class="text-sm">Correo público<input class="field" formControlName="publicEmail" /></label>
          <label class="text-sm md:col-span-2">Contacto público<textarea class="field" rows="3" formControlName="publicContact"></textarea></label>
          @if (message()) { <p class="text-sm md:col-span-2" role="status">{{ message() }}</p> }
          <button class="rounded-full bg-clay px-4 py-2 text-white md:col-span-2" type="submit">Guardar datos</button>
        </form>
      </div>
    }
  `,
  styles: `:host { display: block; } .field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class CompanyProfilePage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  readonly profile = signal<CompanyProfile | null>(null);
  readonly provinces = signal<Province[]>([]);
  readonly preview = signal('');
  readonly file = signal<File | null>(null);
  readonly uploading = signal(false);
  readonly message = signal('');
  readonly logoMessage = signal('');
  readonly form = this.formBuilder.nonNullable.group({
    commercialName: ['', Validators.required], legalName: ['', Validators.required], rnc: [''], phone: [''], email: [''],
    address: [''], provinceCode: [''], municipalityId: [''], website: [''], description: [''], publicPhone: [''], publicEmail: [''], publicContact: [''],
  });

  ngOnInit(): void {
    this.api.locations().subscribe((items) => this.provinces.set(items));
    this.api.company().subscribe((company) => {
      this.profile.set(company);
      this.form.patchValue({
        commercialName: company.commercialName,
        legalName: company.legalName,
        rnc: company.rnc ?? '',
        phone: company.phone ?? '',
        email: company.email ?? '',
        address: company.address ?? '',
        provinceCode: company.provinceCode ?? '',
        municipalityId: company.municipalityId ?? '',
        website: company.website ?? '',
        description: company.description ?? '',
        publicPhone: company.publicPhone ?? '',
        publicEmail: company.publicEmail ?? '',
        publicContact: company.publicContact ?? '',
      });
    });
  }

  municipalities() {
    return this.provinces().find((item) => item.code === this.form.controls.provinceCode.value)?.municipalities ?? [];
  }

  choose(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 2_097_152) {
      this.logoMessage.set('Use PNG, JPEG o WebP de hasta 2 MB.');
      this.file.set(null);
      return;
    }
    this.file.set(file);
    this.logoMessage.set('');
    const reader = new FileReader();
    reader.onload = () => this.preview.set(String(reader.result));
    reader.readAsDataURL(file);
  }

  upload(): void {
    const file = this.file();
    if (!file) return;
    this.uploading.set(true);
    this.api.uploadLogo(file).subscribe({
      next: (company) => { this.profile.set(company); this.preview.set(''); this.file.set(null); this.uploading.set(false); this.logoMessage.set('El logo quedó actualizado.'); },
      error: (error) => { this.uploading.set(false); this.logoMessage.set(problemMessage(error)); },
    });
  }

  remove(): void {
    if (!confirm('¿Eliminar el logo y volver a la imagen predeterminada?')) return;
    this.api.deleteLogo().subscribe({
      next: (company) => { this.profile.set(company); this.preview.set(''); this.logoMessage.set('Se quitó el logo.'); },
      error: (error) => this.logoMessage.set(problemMessage(error)),
    });
  }

  save(): void {
    this.api.updateCompany(this.form.getRawValue()).subscribe({
      next: (company) => { this.profile.set(company); this.message.set('Datos guardados.'); },
      error: (error) => this.message.set(problemMessage(error)),
    });
  }
}

@Component({
  selector: 'app-projects',
  imports: [RouterLink, Loading, EmptyState, Pager],
  template: `
    <div class="flex items-center justify-between"><h1 class="text-4xl">Proyectos</h1><a class="rounded-full bg-sea px-4 py-2 text-sm text-sand" routerLink="/app/proyectos/nuevo">Nuevo proyecto</a></div>
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="No hay proyectos" detail="Cree el primer desarrollo de la constructora." /></div> }
    @else {
      <div class="mt-4 overflow-x-auto rounded-2xl border border-line bg-paper">
        <table class="w-full min-w-[720px] text-left text-sm">
          <thead class="text-ink-soft"><tr><th class="p-3">Nombre</th><th>Ubicación</th><th>Estado</th><th>Publicación</th><th>Desde</th></tr></thead>
          <tbody>
            @for (project of items(); track project.id) {
              <tr class="border-t border-line"><td class="p-3"><a class="underline" [routerLink]="['/app/proyectos', project.id]">{{ project.name }}</a></td><td>{{ project.municipalityName }}</td><td>{{ status(project.status) }}</td><td>{{ publication(project.publicationStatus) }}</td><td>{{ money(project.minPrice, project.currency) }}</td></tr>
            }
          </tbody>
        </table>
      </div>
      <app-pager [page]="page()" [pages]="pages()" [total]="total()" (pageChange)="load($event)" />
    }
  `,
})
export class ProjectsPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<ProjectDetail[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0);
  readonly pages = signal(0);
  readonly total = signal(0);
  ngOnInit(): void { this.load(0); }
  load(page: number): void {
    this.api.projects({ page, size: 20 }).subscribe({
      next: (result) => { this.items.set(result.content); this.page.set(result.page); this.pages.set(result.totalPages); this.total.set(result.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
  status(value: string) { return label(PROJECT_STATUS, value); }
  publication(value: string) { return label(PUBLICATION, value); }
  readonly money = money;
}

@Component({
  selector: 'app-project-editor',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <a class="text-sm text-sea" routerLink="/app/proyectos">Volver a proyectos</a>
    <h1 class="mt-2 text-4xl">{{ id() ? 'Editar proyecto' : 'Nuevo proyecto' }}</h1>
    <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-5 md:grid-cols-2" [formGroup]="form" (ngSubmit)="save()">
      <label class="text-sm">Nombre<input class="field" formControlName="name" /></label>
      <label class="text-sm">Tipo<select class="field" formControlName="developmentType">@for (item of types; track item[0]) { <option [value]="item[0]">{{ item[1] }}</option> }</select></label>
      <label class="text-sm md:col-span-2">Descripción<textarea class="field" rows="4" formControlName="description"></textarea></label>
      <label class="text-sm">Provincia<select class="field" formControlName="provinceCode"><option value="">Seleccione</option>@for (item of provinces(); track item.code) { <option [value]="item.code">{{ item.name }}</option> }</select></label>
      <label class="text-sm">Municipio<select class="field" formControlName="municipalityId"><option value="">Seleccione</option>@for (item of municipalities(); track item.id) { <option [value]="item.id">{{ item.name }}</option> }</select></label>
      <label class="text-sm md:col-span-2">Dirección<input class="field" formControlName="address" /></label>
      <label class="text-sm">Latitud<input class="field" formControlName="latitude" /></label>
      <label class="text-sm">Longitud<input class="field" formControlName="longitude" /></label>
      <label class="text-sm">Inicio estimado<input class="field" type="date" formControlName="estimatedStart" /></label>
      <label class="text-sm">Entrega estimada<input class="field" type="date" formControlName="estimatedDelivery" /></label>
      <label class="text-sm">Estado<select class="field" formControlName="status">@for (item of statuses; track item[0]) { <option [value]="item[0]">{{ item[1] }}</option> }</select></label>
      <label class="text-sm">Precio desde<input class="field" type="number" formControlName="minPrice" /></label>
      <label class="text-sm">Moneda<input class="field" formControlName="currency" maxlength="3" /></label>
      <label class="text-sm">Amenidades, separadas por coma<input class="field" formControlName="amenities" /></label>
      <label class="text-sm">Características, separadas por coma<input class="field" formControlName="features" /></label>
      <label class="text-sm md:col-span-2">Información comercial<textarea class="field" rows="3" formControlName="commercialInfo"></textarea></label>
      <label class="text-sm md:col-span-2">Notas internas<textarea class="field" rows="3" formControlName="internalNotes"></textarea></label>
      @if (message()) { <p class="text-sm md:col-span-2">{{ message() }}</p> }
      <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Guardar</button>
    </form>
    @if (project(); as current) {
      <section class="mt-6 rounded-3xl border border-line bg-paper p-5">
        <h2 class="text-2xl">Publicación</h2>
        <p class="text-sm text-ink-soft">Estado actual: {{ publication(current.publicationStatus) }}. Un borrador no aparece en el marketplace.</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button type="button" class="rounded-full bg-sea px-3 py-1 text-sm text-sand" (click)="setPublication('PUBLISHED', true)">Publicar y destacar</button>
          <button type="button" class="rounded-full border border-line px-3 py-1 text-sm" (click)="setPublication('PUBLISHED', false)">Publicar</button>
          <button type="button" class="rounded-full border border-line px-3 py-1 text-sm" (click)="setPublication('HIDDEN', false)">Ocultar</button>
          <button type="button" class="rounded-full border border-line px-3 py-1 text-sm" (click)="remove()">Eliminar</button>
        </div>
        <h2 class="mt-6 text-2xl">Fotografías</h2>
        <div class="mt-3 grid gap-3 sm:grid-cols-3">
          @for (image of current.images; track image.id) {
            <figure>
              <img class="aspect-[4/3] w-full rounded-2xl object-cover" [src]="image.url" [alt]="image.altText || current.name" />
              <button type="button" class="mt-1 text-sm" (click)="removeImage(image.id)">Quitar</button>
            </figure>
          }
        </div>
        <label class="mt-3 block text-sm">Agregar fotografía
          <input class="mt-1 block" type="file" accept="image/png,image/jpeg,image/webp" (change)="addImage($event)" />
        </label>
        <label class="mt-4 block text-sm">Avisar a compradores<textarea class="field" rows="3" [value]="notice()" (input)="notice.set(textValue($event))"></textarea></label>
        <button type="button" class="mt-2 rounded-full border border-line px-3 py-1 text-sm" (click)="notify()">Enviar aviso</button>
      </section>
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class ProjectEditorPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  readonly id = signal<string | null>(null);
  readonly project = signal<ProjectDetail | null>(null);
  readonly provinces = signal<Province[]>([]);
  readonly message = signal('');
  readonly notice = signal('');
  readonly types = Object.entries(PROJECT_TYPES);
  readonly statuses = Object.entries(PROJECT_STATUS);
  readonly form = this.formBuilder.nonNullable.group({
    name: ['', Validators.required], description: [''], developmentType: ['APARTMENT_BUILDING', Validators.required], provinceCode: ['', Validators.required],
    municipalityId: ['', Validators.required], address: [''], latitude: [''], longitude: [''], estimatedStart: [''], estimatedDelivery: [''],
    status: ['PLANNING', Validators.required], amenities: [''], features: [''], commercialInfo: [''], internalNotes: [''], minPrice: [''], currency: ['DOP'],
  });

  ngOnInit(): void {
    this.api.locations().subscribe((items) => this.provinces.set(items));
    const id = this.route.snapshot.paramMap.get('id');
    if (id && id !== 'nuevo') {
      this.id.set(id);
      this.api.project(id).subscribe((project) => {
        this.project.set(project);
        this.form.patchValue({
          ...project,
          latitude: project.latitude?.toString() ?? '',
          longitude: project.longitude?.toString() ?? '',
          minPrice: project.minPrice?.toString() ?? '',
          amenities: project.amenities.join(', '),
          features: project.features.join(', '),
          estimatedStart: project.estimatedStart ?? '',
          estimatedDelivery: project.estimatedDelivery ?? '',
          description: project.description ?? '',
          address: project.address ?? '',
          commercialInfo: project.commercialInfo ?? '',
          internalNotes: project.internalNotes ?? '',
        });
      });
    }
  }

  municipalities() { return this.provinces().find((item) => item.code === this.form.controls.provinceCode.value)?.municipalities ?? []; }

  save(): void {
    const value = this.form.getRawValue();
    const body = {
      ...value,
      latitude: value.latitude ? Number(value.latitude) : null,
      longitude: value.longitude ? Number(value.longitude) : null,
      minPrice: value.minPrice ? Number(value.minPrice) : null,
      amenities: splitList(value.amenities),
      features: splitList(value.features),
      estimatedStart: value.estimatedStart || null,
      estimatedDelivery: value.estimatedDelivery || null,
    };
    this.api.saveProject(this.id(), body).subscribe({
      next: (project) => { this.project.set(project); this.id.set(project.id); this.message.set('Proyecto guardado.'); void this.router.navigate(['/app/proyectos', project.id]); },
      error: (error) => this.message.set(problemMessage(error)),
    });
  }

  setPublication(publication: string, featured: boolean): void {
    const id = this.id();
    if (!id) return;
    this.api.publish(id, publication, featured).subscribe({
      next: (project) => { this.project.set(project); this.message.set('Publicación actualizada.'); },
      error: (error) => this.message.set(problemMessage(error)),
    });
  }

  remove(): void {
    const id = this.id();
    if (!id || !confirm('¿Eliminar este proyecto? Se conserva el historial si hay ventas, y en ese caso la operación se rechaza.')) return;
    this.api.deleteProject(id).subscribe({ next: () => void this.router.navigateByUrl('/app/proyectos'), error: (error) => this.message.set(problemMessage(error)) });
  }

  addImage(event: Event): void {
    const id = this.id();
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!id || !file) return;
    this.api.uploadProjectImage(id, file, this.project()?.name ?? 'Fotografía').subscribe({
      next: () => this.api.project(id).subscribe((project) => this.project.set(project)),
      error: (error) => this.message.set(problemMessage(error)),
    });
  }

  removeImage(imageId: string): void {
    const id = this.id();
    if (!id) return;
    this.api.deleteProjectImage(id, imageId).subscribe({ next: () => this.api.project(id).subscribe((project) => this.project.set(project)) });
  }

  notify(): void {
    const id = this.id();
    if (!id || !this.notice().trim()) return;
    this.api.notifyBuyers(id, this.notice()).subscribe({ next: () => this.message.set('Aviso enviado a los compradores del proyecto.'), error: (error) => this.message.set(problemMessage(error)) });
  }

  publication(value: string) { return label(PUBLICATION, value); }
  textValue(event: Event) { return (event.target as HTMLTextAreaElement).value; }
}

function splitList(value: string): string[] {
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

@Component({
  selector: 'app-units',
  imports: [ReactiveFormsModule, Loading, EmptyState],
  template: `
    <h1 class="text-4xl">Unidades</h1>
    <label class="mt-4 block text-sm">Proyecto
      <select class="field" [value]="projectId()" (change)="chooseProject($event)">
        <option value="">Seleccione</option>
        @for (project of projects(); track project.id) { <option [value]="project.id">{{ project.name }}</option> }
      </select>
    </label>
    @if (projectId()) {
      <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-4 md:grid-cols-4" [formGroup]="form" (ngSubmit)="save()">
        <label class="text-sm">Código<input class="field" formControlName="code" /></label>
        <label class="text-sm">Tipo<select class="field" formControlName="propertyType">@for (item of types; track item[0]) { <option [value]="item[0]">{{ item[1] }}</option> }</select></label>
        <label class="text-sm">Precio<input class="field" type="number" formControlName="price" /></label>
        <label class="text-sm">Moneda<input class="field" formControlName="currency" /></label>
        <label class="text-sm">Habitaciones<input class="field" type="number" formControlName="bedrooms" /></label>
        <label class="text-sm">Baños<input class="field" formControlName="bathrooms" /></label>
        <label class="text-sm">m²<input class="field" formControlName="areaM2" /></label>
        <label class="text-sm">Nivel<input class="field" formControlName="levelLabel" /></label>
        <label class="text-sm md:col-span-2">Características<input class="field" formControlName="features" /></label>
        <label class="flex items-center gap-2 text-sm"><input type="checkbox" formControlName="publiclyVisible" /> Visible al público</label>
        <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">{{ editing() ? 'Actualizar' : 'Agregar' }}</button>
      </form>
      @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
      @if (loading()) { <app-loading /> }
      @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin unidades" detail="Agregue casas o apartamentos a este proyecto." /></div> }
      @else {
        <div class="mt-4 overflow-x-auto rounded-2xl border border-line bg-paper">
          <table class="w-full min-w-[760px] text-left text-sm">
            <thead class="text-ink-soft"><tr><th class="p-3">Código</th><th>Tipo</th><th>Precio</th><th>Estado</th><th></th></tr></thead>
            <tbody>
              @for (unit of items(); track unit.id) {
                <tr class="border-t border-line">
                  <td class="p-3">{{ unit.code }}</td><td>{{ type(unit.propertyType) }}</td><td>{{ money(unit.price, unit.currency) }}</td><td>{{ availability(unit.availability) }}</td>
                  <td class="space-x-2 p-3 text-right">
                    <button type="button" (click)="edit(unit)">Editar</button>
                    @if (unit.availability === 'AVAILABLE') { <button type="button" (click)="mark(unit, 'OFF_MARKET')">Retirar</button> }
                    @if (unit.availability === 'OFF_MARKET') { <button type="button" (click)="mark(unit, 'AVAILABLE')">Publicar disponibilidad</button> }
                    @if (unit.availability === 'AVAILABLE' || unit.availability === 'OFF_MARKET') { <button type="button" (click)="remove(unit)">Eliminar</button> }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class UnitsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  readonly projects = signal<ProjectDetail[]>([]);
  readonly items = signal<Unit[]>([]);
  readonly projectId = signal('');
  readonly editing = signal<string | null>(null);
  readonly loading = signal(false);
  readonly message = signal('');
  readonly types = Object.entries(PROPERTY_TYPES);
  readonly form = this.formBuilder.nonNullable.group({
    code: ['', Validators.required], propertyType: ['APARTMENT', Validators.required], price: [0, Validators.required], currency: ['DOP'],
    bedrooms: [null as number | null], bathrooms: [''], areaM2: [''], levelLabel: [''], features: [''], publiclyVisible: true, buildingId: [''],
  });

  ngOnInit(): void { this.api.projects({ page: 0, size: 100 }).subscribe((page) => this.projects.set(page.content)); }
  chooseProject(event: Event): void { this.projectId.set((event.target as HTMLSelectElement).value); this.reload(); }
  reload(): void {
    if (!this.projectId()) return;
    this.loading.set(true);
    this.api.unitsOf(this.projectId()).subscribe({ next: (items) => { this.items.set(items); this.loading.set(false); }, error: () => this.loading.set(false) });
  }
  save(): void {
    const value = this.form.getRawValue();
    const body = { ...value, buildingId: value.buildingId || null, bathrooms: value.bathrooms ? Number(value.bathrooms) : null, areaM2: value.areaM2 ? Number(value.areaM2) : null, features: splitList(value.features), price: Number(value.price) };
    this.api.saveUnit(this.projectId(), this.editing(), body).subscribe({
      next: () => { this.editing.set(null); this.form.reset({ propertyType: 'APARTMENT', currency: 'DOP', publiclyVisible: true, price: 0 }); this.message.set('Unidad guardada.'); this.reload(); },
      error: (error) => this.message.set(problemMessage(error)),
    });
  }
  edit(unit: Unit): void {
    this.editing.set(unit.id);
    this.form.patchValue({ ...unit, bathrooms: unit.bathrooms?.toString() ?? '', areaM2: unit.areaM2?.toString() ?? '', features: unit.features.join(', '), levelLabel: unit.levelLabel ?? '', buildingId: unit.buildingId ?? '' });
  }
  mark(unit: Unit, availability: string): void {
    this.api.changeAvailability(unit.id, availability).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) });
  }
  remove(unit: Unit): void {
    if (!confirm(`¿Eliminar la unidad ${unit.code}?`)) return;
    this.api.deleteUnit(unit.id).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) });
  }
  type(value: string) { return label(PROPERTY_TYPES, value); }
  availability(value: string) { return label(AVAILABILITY, value); }
  readonly money = money;
}

@Component({
  selector: 'app-leads',
  imports: [ReactiveFormsModule, RouterLink, Loading, EmptyState, Pager],
  template: `
    <h1 class="text-4xl">Prospectos</h1>
    <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-4 md:grid-cols-4" [formGroup]="form" (ngSubmit)="create()">
      <label class="text-sm">Nombre<input class="field" formControlName="fullName" /></label>
      <label class="text-sm">Correo<input class="field" formControlName="email" /></label>
      <label class="text-sm">Teléfono<input class="field" formControlName="phone" /></label>
      <label class="text-sm">Origen<select class="field" formControlName="source"><option value="MANUAL">Manual</option><option value="PHONE">Teléfono</option><option value="REFERRAL">Referido</option><option value="OTHER">Otro</option></select></label>
      <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Registrar</button>
    </form>
    @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin prospectos" detail="Los formularios del marketplace también aparecen aquí." /></div> }
    @else {
      <div class="mt-4 overflow-x-auto rounded-2xl border border-line bg-paper">
        <table class="w-full min-w-[680px] text-left text-sm"><thead class="text-ink-soft"><tr><th class="p-3">Nombre</th><th>Etapa</th><th>Próximo paso</th><th>Creado</th></tr></thead>
          <tbody>@for (lead of items(); track lead.id) { <tr class="border-t border-line"><td class="p-3"><a class="underline" [routerLink]="['/app/prospectos', lead.id]">{{ lead.fullName }}</a></td><td>{{ lead.stageCode }}</td><td>{{ lead.nextStep || '—' }}</td><td>{{ when(lead.createdAt, true) }}</td></tr> }</tbody>
        </table>
      </div>
      <app-pager [page]="page()" [pages]="pages()" [total]="total()" (pageChange)="load($event)" />
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class LeadsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  readonly items = signal<Lead[]>([]);
  readonly loading = signal(true);
  readonly message = signal('');
  readonly page = signal(0); readonly pages = signal(0); readonly total = signal(0);
  readonly form = this.formBuilder.nonNullable.group({ fullName: ['', Validators.required], email: [''], phone: [''], source: ['MANUAL'] });
  ngOnInit(): void { this.load(0); }
  load(page: number): void {
    this.api.leads({ page, size: 20 }).subscribe({ next: (result) => { this.items.set(result.content); this.page.set(result.page); this.pages.set(result.totalPages); this.total.set(result.totalElements); this.loading.set(false); }, error: () => this.loading.set(false) });
  }
  create(): void {
    this.api.saveLead(null, this.form.getRawValue()).subscribe({ next: () => { this.form.reset({ source: 'MANUAL' }); this.message.set('Prospecto registrado.'); this.load(0); }, error: (error) => this.message.set(problemMessage(error)) });
  }
  readonly when = when;
}

@Component({
  selector: 'app-lead-detail',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <a class="text-sm text-sea" routerLink="/app/prospectos">Volver</a>
    @if (lead(); as current) {
      <h1 class="mt-2 text-4xl">{{ current.fullName }}</h1>
      <p class="text-sm text-ink-soft">{{ current.email }} · {{ current.phone }}</p>
      <form class="mt-4 grid gap-3 md:grid-cols-2" [formGroup]="form" (ngSubmit)="save()">
        <label class="text-sm">Etapa<select class="field" formControlName="stageCode">@for (stage of stages(); track stage.id) { <option [value]="stage.code">{{ stage.name }}</option> }</select></label>
        <label class="text-sm">Próximo paso<input class="field" formControlName="nextStep" /></label>
        <label class="text-sm">Fecha<input class="field" type="date" formControlName="nextStepDate" /></label>
        <label class="text-sm md:col-span-2">Notas<textarea class="field" rows="3" formControlName="notes"></textarea></label>
        <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Guardar seguimiento</button>
      </form>
      <form class="mt-6 grid gap-2" [formGroup]="activity" (ngSubmit)="addActivity()">
        <label class="text-sm">Nueva actividad<select class="field" formControlName="type"><option value="NOTE">Nota</option><option value="CALL">Llamada</option><option value="VISIT">Visita</option><option value="EMAIL">Correo</option><option value="WHATSAPP">WhatsApp</option></select></label>
        <textarea class="field" rows="3" formControlName="description" placeholder="Qué ocurrió y cuál es el siguiente paso"></textarea>
        <button class="w-fit rounded-full border border-line px-3 py-1 text-sm" type="submit">Registrar actividad</button>
      </form>
      <ul class="mt-4 space-y-2">@for (item of activities(); track item.id) { <li class="rounded-2xl bg-paper p-3 text-sm"><strong>{{ item.activityType }}</strong> · {{ when(item.createdAt, true) }}<br />{{ item.description }}</li> }</ul>
      @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class LeadDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  readonly lead = signal<Lead | null>(null);
  readonly activities = signal<{ id: string; activityType: string; description: string; createdAt: string }[]>([]);
  readonly stages = signal<Stage[]>([]);
  readonly message = signal('');
  readonly form = this.formBuilder.nonNullable.group({ fullName: [''], email: [''], phone: [''], stageCode: ['NEW'], nextStep: [''], nextStepDate: [''], notes: [''], projectId: [''], unitId: [''], assignedUserId: [''] });
  readonly activity = this.formBuilder.nonNullable.group({ type: ['NOTE'], description: ['', Validators.required] });
  ngOnInit(): void {
    this.api.pipeline().subscribe((stages) => this.stages.set(stages));
    this.reload();
  }
  reload(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.api.lead(id).subscribe((detail) => {
      this.lead.set(detail.lead);
      this.activities.set(detail.activities);
      this.form.patchValue({ ...detail.lead, nextStep: detail.lead.nextStep ?? '', nextStepDate: detail.lead.nextStepDate ?? '', notes: detail.lead.notes ?? '', email: detail.lead.email ?? '', phone: detail.lead.phone ?? '', projectId: detail.lead.projectId ?? '', unitId: detail.lead.unitId ?? '', assignedUserId: detail.lead.assignedUserId ?? '' });
    });
  }
  save(): void {
    const current = this.lead();
    if (!current) return;
    const value = this.form.getRawValue();
    this.api.saveLead(current.id, { ...value, projectId: value.projectId || null, unitId: value.unitId || null, assignedUserId: value.assignedUserId || null, nextStepDate: value.nextStepDate || null }).subscribe({ next: () => { this.message.set('Seguimiento actualizado.'); this.reload(); }, error: (error) => this.message.set(problemMessage(error)) });
  }
  addActivity(): void {
    const current = this.lead();
    if (!current) return;
    this.api.addActivity(current.id, this.activity.controls.type.value, this.activity.controls.description.value).subscribe({ next: () => { this.activity.reset({ type: 'NOTE', description: '' }); this.reload(); }, error: (error) => this.message.set(problemMessage(error)) });
  }
  readonly when = when;
}

@Component({
  selector: 'app-reservations',
  imports: [ReactiveFormsModule, Loading, EmptyState, Pager],
  template: `
    <h1 class="text-4xl">Reservas</h1>
    <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-4 md:grid-cols-3" [formGroup]="form" (ngSubmit)="create()">
      <label class="text-sm">Unidad
        <select class="field" formControlName="unitId"><option value="">Seleccione</option>@for (unit of units(); track unit.id) { <option [value]="unit.id">{{ unit.code }} · {{ availability(unit.availability) }}</option> }</select>
      </label>
      <label class="text-sm">Prospecto
        <select class="field" formControlName="leadId"><option value="">Ninguno</option>@for (lead of leads(); track lead.id) { <option [value]="lead.id">{{ lead.fullName }}</option> }</select>
      </label>
      <label class="text-sm">Comprador
        <select class="field" formControlName="customerId"><option value="">Ninguno</option>@for (customer of customers(); track customer.id) { <option [value]="customer.id">{{ customer.fullName }}</option> }</select>
      </label>
      <label class="text-sm">Importe<input class="field" type="number" formControlName="amount" /></label>
      <label class="text-sm">Moneda<input class="field" formControlName="currency" /></label>
      <label class="text-sm md:col-span-3">Observaciones<textarea class="field" rows="2" formControlName="notes"></textarea></label>
      <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Crear reserva</button>
    </form>
    @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin reservas" detail="Una reserva temporal bloquea la unidad hasta su vencimiento." /></div> }
    @else {
      <div class="mt-4 overflow-x-auto rounded-2xl border border-line bg-paper">
        <table class="w-full min-w-[680px] text-left text-sm"><thead class="text-ink-soft"><tr><th class="p-3">Unidad</th><th>Vence</th><th>Estado</th><th>Importe</th><th></th></tr></thead>
          <tbody>@for (item of items(); track item.id) { <tr class="border-t border-line"><td class="p-3">{{ unitCode(item.unitId) }}</td><td>{{ when(item.expiresAt, true) }}</td><td>{{ status(item.status) }}</td><td>{{ money(item.amount, item.currency) }}</td><td>@if (item.status === 'ACTIVE') { <button type="button" (click)="cancel(item)">Cancelar</button> }</td></tr> }</tbody>
        </table>
      </div>
      <app-pager [page]="page()" [pages]="pages()" [total]="total()" (pageChange)="load($event)" />
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class ReservationsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  readonly items = signal<Reservation[]>([]);
  readonly units = signal<Unit[]>([]);
  readonly leads = signal<Lead[]>([]);
  readonly customers = signal<Customer[]>([]);
  readonly loading = signal(true);
  readonly message = signal('');
  readonly page = signal(0); readonly pages = signal(0); readonly total = signal(0);
  readonly form = this.formBuilder.nonNullable.group({ unitId: ['', Validators.required], leadId: [''], customerId: [''], amount: [''], currency: ['DOP'], notes: [''] });
  ngOnInit(): void {
    this.load(0);
    this.api.units({ page: 0, size: 100 }).subscribe((page) => this.units.set(page.content));
    this.api.leads({ page: 0, size: 100 }).subscribe((page) => this.leads.set(page.content));
    this.api.customers({ page: 0, size: 100 }).subscribe((page) => this.customers.set(page.content));
  }
  load(page: number): void {
    this.api.reservations({ page, size: 20 }).subscribe({ next: (result) => { this.items.set(result.content); this.page.set(result.page); this.pages.set(result.totalPages); this.total.set(result.totalElements); this.loading.set(false); }, error: () => this.loading.set(false) });
  }
  create(): void {
    const value = this.form.getRawValue();
    this.api.reserve({ ...value, leadId: value.leadId || null, customerId: value.customerId || null, amount: value.amount ? Number(value.amount) : null, expiresAt: null }).subscribe({
      next: () => { this.message.set('Reserva creada. La unidad quedó reservada.'); this.load(0); },
      error: (error) => this.message.set(problemMessage(error)),
    });
  }
  cancel(item: Reservation): void {
    const notes = prompt('Motivo de la cancelación') ?? '';
    if (!notes.trim()) return;
    this.api.cancelReservation(item.id, notes).subscribe({ next: () => this.load(this.page()), error: (error) => this.message.set(problemMessage(error)) });
  }
  unitCode(id: string) { return this.units().find((unit) => unit.id === id)?.code ?? id.slice(0, 8); }
  availability(value: string) { return label(AVAILABILITY, value); }
  status(value: string) { return label(RESERVATION_STATUS, value); }
  readonly money = money;
  readonly when = when;
}
