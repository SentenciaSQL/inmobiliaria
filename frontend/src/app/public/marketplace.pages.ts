import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CompanyPage, ProjectCard, ProjectPage, Province } from '../core/models';
import { PROJECT_STATUS, PROJECT_TYPES, label } from '../core/labels';
import { EmptyState, Loading, Logo, Pager, money, problemMessage, when } from '../shared/ui';

@Component({
  selector: 'app-public-header',
  imports: [RouterLink, Logo],
  template: `
    <header class="border-b border-line bg-paper/90 backdrop-blur">
      <div class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4">
        <a routerLink="/" class="flex items-center gap-3">
          <span class="h-10 w-10 overflow-hidden rounded-xl"><app-logo name="Alzada" alt="Alzada" /></span>
          <span>
            <span class="block font-display text-xl leading-none">Alzada</span>
            <span class="text-xs text-ink-soft">Inmuebles en República Dominicana</span>
          </span>
        </a>
        <nav class="flex items-center gap-4 text-sm">
          <a routerLink="/" class="hover:text-sea">Proyectos</a>
          <a routerLink="/ingresar" class="rounded-full bg-sea px-4 py-2 text-sand">Ingresar</a>
        </nav>
      </div>
    </header>
  `,
})
export class PublicHeader {}

@Component({
  selector: 'app-project-card',
  imports: [RouterLink, Logo],
  template: `
    <a [routerLink]="['/proyectos', project().slug]" class="flex h-full flex-col overflow-hidden rounded-3xl border border-line bg-paper shadow-sm">
      <div class="aspect-[4/3] bg-sea-deep">
        @if (project().coverUrl) {
          <img [src]="project().coverUrl!" [alt]="project().name" class="h-full w-full object-cover" />
        } @else {
          <div class="grid h-full place-items-end bg-[radial-gradient(circle_at_top_left,#1d6a7a,transparent_55%),linear-gradient(#0f4c5c,#0a3642)] p-4 text-sand">
            <span class="text-sm">Sin fotografía publicada</span>
          </div>
        }
      </div>
      <div class="flex flex-1 flex-col gap-2 p-4">
        <div class="flex items-center gap-2 text-xs text-ink-soft">
          <span class="h-7 w-7 overflow-hidden rounded-lg"><app-logo [src]="project().companyLogoUrl" [name]="project().companyName" [alt]="project().companyName" /></span>
          {{ project().companyName }}
        </div>
        <h3 class="font-display text-2xl">{{ project().name }}</h3>
        <p class="text-sm text-ink-soft">{{ project().municipalityName }}, {{ project().provinceName }}</p>
        <p class="mt-auto text-sm">Desde {{ price() }}</p>
      </div>
    </a>
  `,
})
export class ProjectCardView {
  readonly project = input.required<ProjectCard>();
  price() { return money(this.project().minPrice, this.project().currency); }
}


@Component({
  selector: 'app-home',
  imports: [PublicHeader, ReactiveFormsModule, EmptyState, Loading, Pager, ProjectCardView],
  template: `
    <app-public-header />
    <main>
      <section class="bg-sea-deep text-sand">
        <div class="mx-auto grid max-w-6xl gap-8 px-4 py-16 md:grid-cols-[1.2fr_0.8fr] md:items-end">
          <div>
            <p class="text-sm uppercase tracking-[0.18em] text-sand/70">Marketplace inmobiliario</p>
            <h1 class="mt-3 text-4xl leading-tight md:text-6xl">Proyectos de constructoras en todo el país</h1>
            <p class="mt-4 max-w-xl text-sand/80">Busque apartamentos, casas y desarrollos publicados por cada constructora. Los precios se muestran en pesos dominicanos.</p>
          </div>
          <form class="grid gap-3 rounded-3xl bg-paper p-4 text-ink shadow-xl" [formGroup]="filters" (ngSubmit)="search(0)">
            <label class="text-sm">Buscar
              <input class="mt-1 w-full rounded-xl border border-line px-3 py-2" formControlName="q" placeholder="Nombre o municipio" />
            </label>
            <div class="grid gap-3 sm:grid-cols-2">
              <label class="text-sm">Provincia
                <select class="mt-1 w-full rounded-xl border border-line px-3 py-2" formControlName="province" (change)="search(0)">
                  <option value="">Todas</option>
                  @for (province of provinces(); track province.code) {
                    <option [value]="province.code">{{ province.name }}</option>
                  }
                </select>
              </label>
              <label class="text-sm">Municipio
                <select class="mt-1 w-full rounded-xl border border-line px-3 py-2" formControlName="municipalityId" (change)="search(0)">
                  <option value="">Todos</option>
                  @for (municipality of municipalities(); track municipality.id) {
                    <option [value]="municipality.id">{{ municipality.name }}</option>
                  }
                </select>
              </label>
              <label class="text-sm">Precio mínimo
                <input class="mt-1 w-full rounded-xl border border-line px-3 py-2" type="number" min="0" formControlName="minPrice" />
              </label>
              <label class="text-sm">Precio máximo
                <input class="mt-1 w-full rounded-xl border border-line px-3 py-2" type="number" min="0" formControlName="maxPrice" />
              </label>
              <label class="text-sm">Tipo
                <select class="mt-1 w-full rounded-xl border border-line px-3 py-2" formControlName="type">
                  <option value="">Todos</option>
                  @for (item of typeOptions; track item[0]) { <option [value]="item[0]">{{ item[1] }}</option> }
                </select>
              </label>
              <label class="text-sm">Estado del proyecto
                <select class="mt-1 w-full rounded-xl border border-line px-3 py-2" formControlName="status">
                  <option value="">Todos</option>
                  @for (item of statusOptions; track item[0]) { <option [value]="item[0]">{{ item[1] }}</option> }
                </select>
              </label>
            </div>
            <label class="flex items-center gap-2 text-sm"><input type="checkbox" formControlName="available" /> Solo con unidades disponibles</label>
            <button class="rounded-full bg-clay px-4 py-2 text-white" type="submit">Buscar proyectos</button>
          </form>
        </div>
      </section>
      <section class="mx-auto max-w-6xl px-4 py-12">
        <h2 class="text-3xl">Destacados</h2>
        @if (loadingFeatured()) { <app-loading /> }
        @else if (featured().length === 0) {
          <div class="mt-4"><app-empty title="Aún no hay proyectos destacados" detail="Las constructoras deciden qué proyectos se publican." /></div>
        } @else {
          <div class="mt-6 grid gap-5 md:grid-cols-3">
            @for (project of featured(); track project.id) { <app-project-card [project]="project" /> }
          </div>
        }
        <h2 class="mt-12 text-3xl">Resultados</h2>
        @if (loading()) { <app-loading /> }
        @else if (results().length === 0) {
          <div class="mt-4"><app-empty title="Ningún proyecto coincide" detail="Ajuste la provincia, el precio o el tipo de propiedad." /></div>
        } @else {
          <div class="mt-6 grid gap-5 md:grid-cols-3">
            @for (project of results(); track project.id) { <app-project-card [project]="project" /> }
          </div>
          <app-pager [page]="page()" [pages]="totalPages()" [total]="total()" (pageChange)="search($event)" />
        }
      </section>
    </main>
  `,
})
export class HomePage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly form = inject(FormBuilder);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  readonly provinces = signal<Province[]>([]);
  readonly featured = signal<ProjectCard[]>([]);
  readonly results = signal<ProjectCard[]>([]);
  readonly loading = signal(true);
  readonly loadingFeatured = signal(true);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly total = signal(0);
  readonly typeOptions = Object.entries(PROJECT_TYPES);
  readonly statusOptions = Object.entries(PROJECT_STATUS);
  readonly filters = this.form.nonNullable.group({
    q: '',
    province: '',
    municipalityId: '',
    minPrice: '',
    maxPrice: '',
    type: '',
    status: '',
    available: false,
  });

  ngOnInit(): void {
    this.title.setTitle('Alzada | Proyectos inmobiliarios en República Dominicana');
    this.meta.updateTag({ name: 'description', content: 'Marketplace de proyectos inmobiliarios publicados por constructoras en República Dominicana. Precios en pesos dominicanos.' });
    this.api.locations().subscribe((items) => this.provinces.set(items));
    this.api.featured().subscribe({
      next: (items) => { this.featured.set(items); this.loadingFeatured.set(false); },
      error: () => this.loadingFeatured.set(false),
    });
    this.search(0);
  }

  municipalities() {
    const code = this.filters.controls.province.value;
    return this.provinces().find((item) => item.code === code)?.municipalities ?? [];
  }

  search(page: number): void {
    const value = this.filters.getRawValue();
    this.loading.set(true);
    this.api.publicProjects({
      q: value.q,
      province: value.province,
      municipalityId: value.municipalityId,
      minPrice: value.minPrice,
      maxPrice: value.maxPrice,
      type: value.type,
      status: value.status,
      availability: value.available ? 'AVAILABLE' : null,
      page,
      size: 12,
    }).subscribe({
      next: (result) => {
        this.results.set(result.content);
        this.page.set(result.page);
        this.totalPages.set(result.totalPages);
        this.total.set(result.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}

@Component({
  selector: 'app-project-detail',
  imports: [PublicHeader, RouterLink, ReactiveFormsModule, Logo, Loading],
  template: `
    <app-public-header />
    @if (loading()) { <app-loading /> }
    @else if (page(); as project) {
      <main class="mx-auto max-w-6xl px-4 py-8">
        <p class="text-sm text-ink-soft">{{ project.summary.municipalityName }}, {{ project.summary.provinceName }}</p>
        <div class="mt-2 flex flex-wrap items-end justify-between gap-4">
          <h1 class="text-4xl md:text-5xl">{{ project.summary.name }}</h1>
          <p class="text-lg">Desde {{ price(project) }}</p>
        </div>
        <a class="mt-3 inline-flex items-center gap-2 text-sm" [routerLink]="['/constructoras', project.company.slug]">
          <span class="h-8 w-8 overflow-hidden rounded-lg"><app-logo [src]="project.company.logoUrl" [name]="project.company.commercialName" [alt]="project.company.commercialName" /></span>
          {{ project.company.commercialName }}
        </a>
        <div class="mt-6 grid gap-3 md:grid-cols-3">
          @if (project.images.length === 0) {
            <div class="aspect-[4/3] rounded-3xl bg-[radial-gradient(circle_at_top_left,#1d6a7a,transparent_55%),linear-gradient(#0f4c5c,#0a3642)] md:col-span-3"></div>
          }
          @for (image of project.images; track image.id) {
            <img class="aspect-[4/3] w-full rounded-3xl object-cover" [src]="image.url" [alt]="image.altText || project.summary.name" />
          }
        </div>
        <div class="mt-8 grid gap-8 lg:grid-cols-[1.4fr_0.8fr]">
          <article class="space-y-6">
            <p class="text-lg leading-relaxed">{{ project.description }}</p>
            <dl class="grid gap-3 text-sm sm:grid-cols-2">
              <div><dt class="text-ink-soft">Tipo</dt><dd>{{ type(project.summary.developmentType) }}</dd></div>
              <div><dt class="text-ink-soft">Estado</dt><dd>{{ status(project.summary.status) }}</dd></div>
              <div><dt class="text-ink-soft">Entrega estimada</dt><dd>{{ date(project.estimatedDelivery) }}</dd></div>
              <div><dt class="text-ink-soft">Ubicación</dt><dd>{{ project.address || project.summary.municipalityName }}</dd></div>
            </dl>
            @if (project.amenities.length) {
              <div><h2 class="text-2xl">Amenidades</h2><ul class="mt-2 flex flex-wrap gap-2">@for (item of project.amenities; track item) { <li class="rounded-full bg-white px-3 py-1 text-sm">{{ item }}</li> }</ul></div>
            }
            @if (project.features.length) {
              <div><h2 class="text-2xl">Características</h2><ul class="mt-2 list-disc pl-5 text-sm">@for (item of project.features; track item) { <li>{{ item }}</li> }</ul></div>
            }
            @if (project.buildings.length) {
              <div>
                <h2 class="text-2xl">Edificios y bloques</h2>
                <ul class="mt-2 space-y-2 text-sm">
                  @for (building of project.buildings; track building.id) {
                    <li class="rounded-2xl bg-paper px-3 py-2">{{ building.name }} · {{ building.code }}@if (building.floors) { · {{ building.floors }} niveles }</li>
                  }
                </ul>
              </div>
            }
            <div>
              <h2 class="text-2xl">Unidades publicadas</h2>
              @if (project.units.length === 0) { <p class="mt-2 text-sm text-ink-soft">No hay unidades visibles en este momento.</p> }
              <div class="mt-3 overflow-x-auto">
                <table class="w-full min-w-[640px] text-left text-sm">
                  <thead class="text-ink-soft"><tr><th class="py-2">Código</th><th>Edificio</th><th>Tipo</th><th>Hab.</th><th>Baños</th><th>m²</th><th>Precio</th><th>Estado</th></tr></thead>
                  <tbody>
                    @for (unit of project.units; track unit.id) {
                      <tr class="border-t border-line">
                        <td class="py-2">{{ unit.code }}</td>
                        <td>{{ unit.buildingName || '—' }}</td>
                        <td>{{ unit.propertyType }}</td>
                        <td>{{ unit.bedrooms ?? '—' }}</td>
                        <td>{{ unit.bathrooms ?? '—' }}</td>
                        <td>{{ unit.areaM2 ?? '—' }}</td>
                        <td>{{ money(unit.price, unit.currency) }}</td>
                        <td>{{ unit.availability }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          </article>
          <aside class="h-fit rounded-3xl border border-line bg-paper p-5">
            <h2 class="text-2xl">Solicitar información</h2>
            <form class="mt-4 grid gap-3" [formGroup]="inquiry" (ngSubmit)="send()">
              <label class="text-sm">Nombre<input class="mt-1 w-full rounded-xl border border-line px-3 py-2" formControlName="name" /></label>
              <label class="text-sm">Correo<input class="mt-1 w-full rounded-xl border border-line px-3 py-2" type="email" formControlName="email" /></label>
              <label class="text-sm">Teléfono<input class="mt-1 w-full rounded-xl border border-line px-3 py-2" formControlName="phone" /></label>
              <label class="text-sm">Unidad de interés
                <select class="mt-1 w-full rounded-xl border border-line px-3 py-2" formControlName="unitId">
                  <option value="">El proyecto en general</option>
                  @for (unit of project.units; track unit.id) { <option [value]="unit.id">{{ unit.code }}</option> }
                </select>
              </label>
              <label class="text-sm">Mensaje<textarea class="mt-1 w-full rounded-xl border border-line px-3 py-2" rows="4" formControlName="message"></textarea></label>
              @if (message()) { <p class="text-sm">{{ message() }}</p> }
              <button class="rounded-full bg-sea px-4 py-2 text-sand disabled:opacity-50" [disabled]="inquiry.invalid || sending()" type="submit">Enviar solicitud</button>
            </form>
            <div class="mt-4 space-y-1 text-sm">
              @if (project.company.publicPhone) { <a class="block" [href]="'tel:' + project.company.publicPhone">Llamar {{ project.company.publicPhone }}</a> }
              @if (project.company.publicEmail) { <a class="block" [href]="'mailto:' + project.company.publicEmail">Escribir a {{ project.company.publicEmail }}</a> }
            </div>
          </aside>
        </div>
      </main>
    }
  `,
})
export class ProjectDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly form = inject(FormBuilder);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  readonly page = signal<ProjectPage | null>(null);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly message = signal('');
  readonly inquiry = this.form.nonNullable.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    unitId: [''],
    message: ['Quisiera coordinar una visita.', Validators.required],
  });

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.api.publicProject(slug).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading.set(false);
        this.title.setTitle(`${page.summary.name} | Alzada`);
        this.meta.updateTag({ name: 'description', content: page.description || `Proyecto ${page.summary.name} en ${page.summary.municipalityName}.` });
      },
      error: () => this.loading.set(false),
    });
  }

  send(): void {
    const project = this.page();
    if (!project || this.inquiry.invalid) return;
    const value = this.inquiry.getRawValue();
    this.sending.set(true);
    this.api.inquire(project.summary.slug, { ...value, unitId: value.unitId || null }).subscribe({
      next: () => { this.sending.set(false); this.message.set('Recibimos su solicitud. La constructora le contactará.'); this.inquiry.reset({ message: 'Quisiera coordinar una visita.' }); },
      error: (error) => { this.sending.set(false); this.message.set(problemMessage(error)); },
    });
  }

  price(project: ProjectPage) { return money(project.summary.minPrice, project.summary.currency); }
  type(value: string) { return label(PROJECT_TYPES, value); }
  status(value: string) { return label(PROJECT_STATUS, value); }
  date(value: string | null) { return when(value); }
  readonly money = money;
}

@Component({
  selector: 'app-company-public',
  imports: [PublicHeader, Logo, ProjectCardView, Loading],
  template: `
    <app-public-header />
    @if (loading()) { <app-loading /> }
    @else if (company(); as profile) {
      <main class="mx-auto max-w-6xl px-4 py-10">
        <div class="flex items-center gap-4">
          <span class="h-20 w-20 overflow-hidden rounded-2xl border border-line bg-white"><app-logo [src]="profile.logoUrl" [name]="profile.commercialName" [alt]="profile.commercialName" /></span>
          <div>
            <h1 class="text-4xl">{{ profile.commercialName }}</h1>
            <p class="text-sm text-ink-soft">{{ profile.municipalityName }} {{ profile.provinceName }}</p>
          </div>
        </div>
        <p class="mt-6 max-w-3xl leading-relaxed">{{ profile.description }}</p>
        <div class="mt-4 text-sm">
          @if (profile.publicPhone) { <a class="mr-4" [href]="'tel:' + profile.publicPhone">{{ profile.publicPhone }}</a> }
          @if (profile.publicEmail) { <a class="mr-4" [href]="'mailto:' + profile.publicEmail">{{ profile.publicEmail }}</a> }
          @if (profile.website) { <a [href]="profile.website" target="_blank" rel="noopener">Sitio web</a> }
        </div>
        @if (profile.publicContact) { <p class="mt-3 text-sm text-ink-soft">{{ profile.publicContact }}</p> }
        <h2 class="mt-10 text-3xl">Proyectos publicados</h2>
        <div class="mt-5 grid gap-5 md:grid-cols-3">
          @for (project of profile.projects; track project.id) { <app-project-card [project]="project" /> }
        </div>
      </main>
    }
  `,
})
export class CompanyPublicPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly title = inject(Title);
  readonly company = signal<CompanyPage | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.api.publicCompany(slug).subscribe({
      next: (company) => { this.company.set(company); this.loading.set(false); this.title.setTitle(`${company.commercialName} | Alzada`); },
      error: () => this.loading.set(false),
    });
  }
}
