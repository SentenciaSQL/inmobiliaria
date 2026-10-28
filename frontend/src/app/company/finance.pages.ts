import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CONTRACT_STATUS, INSTALLMENT_KIND, INSTALLMENT_STATUS, PAYMENT_STATUS, ROLE_LABELS, label } from '../core/labels';
import { AuditEvent, Contract, ContractParty, Customer, DocumentItem, Member, Notice, Payment, PlanView, ProjectDetail, TemplateView, Unit } from '../core/models';
import { SessionService } from '../core/session.service';
import { EmptyState, Loading, Logo, Pager, money, problemMessage, when } from '../shared/ui';

@Component({
  selector: 'app-customers',
  imports: [ReactiveFormsModule, Loading, EmptyState, Pager],
  template: `
    <h1 class="text-4xl">Compradores</h1>
    <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-4 md:grid-cols-3" [formGroup]="form" (ngSubmit)="save()">
      <label class="text-sm">Nombre<input class="field" formControlName="fullName" /></label>
      <label class="text-sm">Correo<input class="field" formControlName="email" /></label>
      <label class="text-sm">Teléfono<input class="field" formControlName="phone" /></label>
      <label class="text-sm">Cédula o RNC<input class="field" formControlName="nationalId" /></label>
      <label class="text-sm md:col-span-2">Dirección<input class="field" formControlName="address" /></label>
      <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">{{ editing() ? 'Actualizar' : 'Registrar' }}</button>
    </form>
    @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin compradores" detail="Registre a quien firmará un contrato." /></div> }
    @else {
      <div class="mt-4 overflow-x-auto rounded-2xl border border-line bg-paper"><table class="w-full min-w-[640px] text-left text-sm"><thead class="text-ink-soft"><tr><th class="p-3">Nombre</th><th>Correo</th><th>Portal</th><th></th></tr></thead>
        <tbody>@for (item of items(); track item.id) { <tr class="border-t border-line"><td class="p-3">{{ item.fullName }}</td><td>{{ item.email }}</td><td>{{ item.userId ? 'Activo' : 'Sin acceso' }}</td><td class="space-x-2 text-right"><button type="button" (click)="edit(item)">Editar</button>@if (!item.userId && admin()) { <button type="button" (click)="portal(item)">Dar acceso</button> }</td></tr> }</tbody></table></div>
      <app-pager [page]="page()" [pages]="pages()" [total]="total()" (pageChange)="load($event)" />
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class CustomersPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly formBuilder = inject(FormBuilder);
  readonly items = signal<Customer[]>([]);
  readonly loading = signal(true);
  readonly message = signal('');
  readonly editing = signal<string | null>(null);
  readonly page = signal(0); readonly pages = signal(0); readonly total = signal(0);
  readonly form = this.formBuilder.nonNullable.group({ fullName: ['', Validators.required], email: [''], phone: [''], nationalId: [''], address: [''] });
  ngOnInit(): void { this.load(0); }
  admin() { return this.session.company()?.role === 'COMPANY_ADMIN'; }
  load(page: number): void { this.api.customers({ page, size: 20 }).subscribe({ next: (result) => { this.items.set(result.content); this.page.set(result.page); this.pages.set(result.totalPages); this.total.set(result.totalElements); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  save(): void {
    this.api.saveCustomer(this.editing(), this.form.getRawValue()).subscribe({ next: () => { this.editing.set(null); this.form.reset(); this.message.set('Comprador guardado.'); this.load(0); }, error: (error) => this.message.set(problemMessage(error)) });
  }
  edit(item: Customer): void { this.editing.set(item.id); this.form.patchValue({ fullName: item.fullName, email: item.email ?? '', phone: item.phone ?? '', nationalId: item.nationalId ?? '', address: item.address ?? '' }); }
  portal(item: Customer): void {
    const password = prompt('Contraseña inicial del portal. Mínimo 10 caracteres, con una letra y un número.') ?? '';
    if (password.length < 10) return;
    this.api.grantPortal(item.id, password).subscribe({ next: () => { this.message.set('El comprador ya puede ingresar al portal.'); this.load(this.page()); }, error: (error) => this.message.set(problemMessage(error)) });
  }
}

@Component({
  selector: 'app-contracts',
  imports: [ReactiveFormsModule, RouterLink, Loading, EmptyState, Pager],
  template: `
    <h1 class="text-4xl">Contratos</h1>
    @if (canWrite()) {
      <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-4 md:grid-cols-3" [formGroup]="form" (ngSubmit)="create()">
        <label class="text-sm">Proyecto<select class="field" formControlName="projectId" (change)="loadUnits()"><option value="">Seleccione</option>@for (project of projects(); track project.id) { <option [value]="project.id">{{ project.name }}</option> }</select></label>
        <label class="text-sm">Unidad<select class="field" formControlName="unitId"><option value="">Seleccione</option>@for (unit of units(); track unit.id) { <option [value]="unit.id">{{ unit.code }}</option> }</select></label>
        <label class="text-sm">Comprador<select class="field" formControlName="customerId"><option value="">Después</option>@for (customer of customers(); track customer.id) { <option [value]="customer.id">{{ customer.fullName }}</option> }</select></label>
        <label class="text-sm">Precio acordado<input class="field" type="number" formControlName="agreedPrice" /></label>
        <label class="text-sm">Inicial<input class="field" type="number" formControlName="agreedDownPayment" /></label>
        <label class="text-sm">Moneda<input class="field" formControlName="currency" /></label>
        <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Crear borrador</button>
      </form>
    }
    @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin contratos" detail="El precio acordado queda fijo aunque cambie el precio publicado." /></div> }
    @else {
      <div class="mt-4 overflow-x-auto rounded-2xl border border-line bg-paper"><table class="w-full min-w-[640px] text-left text-sm"><thead class="text-ink-soft"><tr><th class="p-3">Fecha</th><th>Precio</th><th>Estado</th></tr></thead>
        <tbody>@for (item of items(); track item.id) { <tr class="border-t border-line"><td class="p-3"><a class="underline" [routerLink]="['/app/contratos', item.id]">{{ when(item.agreementDate) }}</a></td><td>{{ money(item.agreedPrice, item.currency) }}</td><td>{{ status(item.status) }}</td></tr> }</tbody></table></div>
      <app-pager [page]="page()" [pages]="pages()" [total]="total()" (pageChange)="load($event)" />
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class ContractsPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly formBuilder = inject(FormBuilder);
  readonly items = signal<Contract[]>([]);
  readonly projects = signal<ProjectDetail[]>([]);
  readonly units = signal<Unit[]>([]);
  readonly customers = signal<Customer[]>([]);
  readonly loading = signal(true);
  readonly message = signal('');
  readonly page = signal(0); readonly pages = signal(0); readonly total = signal(0);
  readonly form = this.formBuilder.nonNullable.group({ projectId: ['', Validators.required], unitId: ['', Validators.required], customerId: [''], agreedPrice: [0, Validators.required], agreedDownPayment: [0], currency: ['DOP'], agreementDate: [''], notes: [''] });
  ngOnInit(): void {
    this.load(0);
    if (this.canWrite()) {
      this.api.projects({ page: 0, size: 100 }).subscribe((page) => this.projects.set(page.content));
      this.api.customers({ page: 0, size: 100 }).subscribe((page) => this.customers.set(page.content));
    }
  }
  canWrite() { const role = this.session.company()?.role; return role === 'COMPANY_ADMIN' || role === 'FINANCE'; }
  load(page: number): void { this.api.contracts({ page, size: 20 }).subscribe({ next: (result) => { this.items.set(result.content); this.page.set(result.page); this.pages.set(result.totalPages); this.total.set(result.totalElements); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  loadUnits(): void { const id = this.form.controls.projectId.value; if (id) this.api.unitsOf(id).subscribe((units) => this.units.set(units)); }
  create(): void {
    const value = this.form.getRawValue();
    this.api.saveContract(null, { ...value, customerId: value.customerId || null, agreedPrice: Number(value.agreedPrice), agreedDownPayment: Number(value.agreedDownPayment), agreementDate: value.agreementDate || null }).subscribe({
      next: () => { this.message.set('Borrador creado. No es una venta hasta activarlo.'); this.load(0); },
      error: (error) => this.message.set(problemMessage(error)),
    });
  }
  status(value: string) { return label(CONTRACT_STATUS, value); }
  readonly money = money; readonly when = when;
}

type ExtraControls = {
  label: FormControl<string>;
  dueDate: FormControl<string>;
  amount: FormControl<number>;
};

@Component({
  selector: 'app-contract-detail',
  imports: [ReactiveFormsModule, RouterLink, Logo],
  template: `
    <a class="no-print text-sm text-sea" routerLink="/app/contratos">Volver</a>
    @if (contract(); as current) {
      <article class="mt-3 rounded-3xl border border-line bg-paper p-5" id="print-contract">
        <div class="flex items-center gap-3">
          <span class="h-14 w-14 overflow-hidden rounded-xl"><app-logo [src]="brand()?.logoUrl ?? null" [name]="brand()?.commercialName || 'Constructora'" [alt]="brand()?.commercialName || 'Logo'" /></span>
          <div><p class="text-sm text-ink-soft">{{ brand()?.commercialName }}</p><h1 class="text-3xl">Contrato {{ status(current.status) }}</h1></div>
        </div>
        <p class="mt-4">Precio acordado {{ money(current.agreedPrice, current.currency) }}. Inicial {{ money(current.agreedDownPayment, current.currency) }}. Fecha {{ when(current.agreementDate) }}.</p>
        <p class="text-sm text-ink-soft">Este registro no constituye firma electrónica ni validez legal automática.</p>
        <button type="button" class="no-print mt-3 rounded-full border border-line px-3 py-1 text-sm" (click)="print()">Imprimir</button>
      </article>
      @if (canWrite() && current.status === 'DRAFT') {
        <section class="no-print mt-4 rounded-3xl border border-line bg-paper p-4">
          <h2 class="text-2xl">Compradores</h2>
          <ul class="mt-2 text-sm">@for (party of parties(); track party.id) { <li>{{ customerName(party.customerId) }} <button type="button" (click)="removeParty(party)">Quitar</button></li> }</ul>
          <div class="mt-2 flex gap-2"><select class="field" [value]="customerId()" (change)="customerId.set(selectValue($event))"><option value="">Seleccione</option>@for (customer of customers(); track customer.id) { <option [value]="customer.id">{{ customer.fullName }}</option> }</select><button type="button" class="rounded-full bg-sea px-3 text-sm text-sand" (click)="addParty()">Agregar</button></div>
        </section>
      }
      <section class="mt-4 rounded-3xl border border-line bg-paper p-4">
        <h2 class="text-2xl">Plan de pagos</h2>
        @if (plan(); as view) {
          <p class="text-sm">Pagado {{ money(view.paid, current.currency) }} · Saldo {{ money(view.balance, current.currency) }}</p>
          <div class="mt-3 overflow-x-auto"><table class="w-full min-w-[720px] text-left text-sm"><thead class="text-ink-soft"><tr><th>Tipo</th><th>Cuota</th><th>Vence</th><th>Monto</th><th>Pagado</th><th>Estado</th></tr></thead>
            <tbody>@for (row of view.installments; track row.id) { <tr class="border-t border-line"><td class="py-2">{{ kind(row.kind) }}</td><td>{{ row.label }}</td><td>{{ when(row.dueDate) }}</td><td>{{ money(row.amount, current.currency) }}</td><td>{{ money(row.paidAmount, current.currency) }}</td><td>{{ installment(row.status) }}</td></tr> }</tbody></table></div>
        }
        @if (canWrite() && planLocked()) {
          <p class="no-print mt-3 text-sm text-ink-soft">Este plan no se puede regenerar porque ya hay pagos confirmados o reportados.</p>
        } @else if (canWrite() && (current.status === 'DRAFT' || current.status === 'ACTIVE')) {
          <form class="no-print mt-4 grid gap-3 md:grid-cols-3" [formGroup]="planForm" (ngSubmit)="savePlan()">
            <label class="text-sm">Total<input class="field" type="number" formControlName="totalPrice" /></label>
            <label class="text-sm">Inicial<input class="field" type="number" formControlName="downPayment" /></label>
            <label class="text-sm">Vence inicial<input class="field" type="date" formControlName="downPaymentDueDate" /></label>
            <label class="text-sm">Cuotas<input class="field" type="number" formControlName="installmentCount" /></label>
            <label class="text-sm">Frecuencia<select class="field" formControlName="frequency"><option value="MONTHLY">Mensual</option><option value="BIWEEKLY">Quincenal</option><option value="WEEKLY">Semanal</option><option value="QUARTERLY">Trimestral</option></select></label>
            <label class="text-sm">Primera cuota<input class="field" type="date" formControlName="firstDueDate" /></label>
            <label class="text-sm">Contra entrega<input class="field" type="number" formControlName="deliveryAmount" /></label>
            <label class="text-sm">Vence entrega<input class="field" type="date" formControlName="deliveryDueDate" /></label>
            <div class="md:col-span-3 rounded-2xl border border-line p-3" formArrayName="extraordinary">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <p class="text-sm font-medium">Cuotas extraordinarias</p>
                <button type="button" class="rounded-full border border-line px-3 py-1 text-sm" (click)="addExtra()">Agregar</button>
              </div>
              <p class="mt-1 text-sm text-ink-soft">Estos montos se restan del saldo que se reparte en las cuotas regulares. Saldo regular: {{ money(regularBalance(), current.currency) }}.</p>
              @for (row of extraordinary().controls; track $index) {
                <div class="mt-2 grid items-end gap-2 md:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)_minmax(0,1fr)_auto]" [formGroupName]="$index">
                  <label class="text-sm">Concepto<input class="field" formControlName="label" maxlength="120" /></label>
                  <label class="text-sm">Vence<input class="field" type="date" formControlName="dueDate" /></label>
                  <label class="text-sm">Monto<input class="field" type="number" formControlName="amount" /></label>
                  <button type="button" class="rounded-full border border-line px-3 py-2 text-sm" (click)="removeExtra($index)">Quitar</button>
                </div>
              }
            </div>
            <label class="text-sm md:col-span-3">Condiciones<textarea class="field" rows="2" formControlName="conditions"></textarea></label>
            <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Generar calendario</button>
          </form>
        }
      </section>
      <section class="no-print mt-4 rounded-3xl border border-line bg-paper p-4">
        <h2 class="text-2xl">Pagos</h2>
        <ul class="mt-2 space-y-2 text-sm">@for (payment of plan()?.payments ?? []; track payment.id) {
          <li class="rounded-xl border border-line p-3">{{ money(payment.amount, payment.currency) }} · {{ paymentStatus(payment.status) }} · {{ payment.referenceNumber || 'Sin referencia' }}
            @if (canWrite() && payment.status === 'REPORTED') { <button type="button" (click)="validate(payment)">Confirmar</button> <button type="button" (click)="reject(payment)">Rechazar</button> }
            @if (canWrite() && payment.status === 'VALIDATED') { <button type="button" (click)="reverse(payment)">Reversar</button> }
          </li>
        }</ul>
        @if (canWrite() && current.status === 'ACTIVE') {
          <form class="mt-3 grid gap-2 md:grid-cols-4" [formGroup]="paymentForm" (ngSubmit)="register()">
            <input class="field" type="number" placeholder="Monto" formControlName="amount" />
            <input class="field" placeholder="Referencia" formControlName="reference" />
            <input class="field" placeholder="Notas" formControlName="notes" />
            <input type="file" (change)="receipt.set(fileOf($event))" />
            <button class="rounded-full bg-clay px-4 py-2 text-white" type="submit">Registrar pago confirmado</button>
          </form>
        }
        <div class="mt-4 flex flex-wrap gap-2">
          @if (canWrite() && current.status === 'DRAFT') { <button type="button" class="rounded-full bg-sea px-3 py-1 text-sm text-sand" (click)="activate()">Activar contrato</button> }
          @if (canWrite() && current.status === 'ACTIVE') { <button type="button" class="rounded-full border border-line px-3 py-1 text-sm" (click)="complete()">Marcar completado</button> }
          @if (canWrite() && current.status !== 'ANNULLED' && current.status !== 'COMPLETED') { <button type="button" class="rounded-full border border-line px-3 py-1 text-sm" (click)="annul()">Anular</button> }
        </div>
        <h2 class="mt-6 text-2xl">Documentos</h2>
        <ul class="mt-2 text-sm">@for (document of documents(); track document.id) { <li><button type="button" class="underline" (click)="download(document)">{{ document.fileName }}</button> · {{ document.visibility === 'BUYER_VISIBLE' ? 'Visible al comprador' : 'Interno' }}</li> }</ul>
        @if (canWrite()) {
          <label class="mt-2 block text-sm">Adjuntar<input type="file" (change)="uploadDoc($event)" /></label>
          <label class="mt-1 flex items-center gap-2 text-sm"><input type="checkbox" [checked]="visible()" (change)="visible.set(checked($event))" /> Visible para el comprador</label>
        }
        @if (message()) { <p class="mt-3 text-sm">{{ message() }}</p> }
      </section>
    }
  `,
  styles: `.field { width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class ContractDetailPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(SessionService);
  private readonly formBuilder = inject(FormBuilder);
  readonly contract = signal<Contract | null>(null);
  readonly parties = signal<ContractParty[]>([]);
  readonly plan = signal<PlanView | null>(null);
  readonly customers = signal<Customer[]>([]);
  readonly documents = signal<DocumentItem[]>([]);
  readonly brand = signal<{ commercialName: string; logoUrl: string | null } | null>(null);
  readonly message = signal('');
  readonly customerId = signal('');
  readonly visible = signal(false);
  readonly receipt = signal<File | null>(null);
  readonly planForm = this.formBuilder.nonNullable.group({
    totalPrice: [0],
    downPayment: [0],
    downPaymentDueDate: [''],
    installmentCount: [12],
    frequency: ['MONTHLY'],
    firstDueDate: [''],
    deliveryAmount: [''],
    deliveryDueDate: [''],
    conditions: [''],
    extraordinary: this.formBuilder.nonNullable.array<FormGroup<ExtraControls>>([]),
  });
  readonly paymentForm = this.formBuilder.nonNullable.group({ amount: [0, Validators.required], reference: [''], notes: [''] });

  ngOnInit(): void { this.reload(); this.api.customers({ page: 0, size: 100 }).subscribe((page) => this.customers.set(page.content)); this.api.brand().subscribe((brand) => this.brand.set(brand)); }
  canWrite() { const role = this.session.company()?.role; return role === 'COMPANY_ADMIN' || role === 'FINANCE'; }
  planLocked(): boolean {
    return (this.plan()?.payments ?? []).some((payment) => payment.status === 'VALIDATED' || payment.status === 'REPORTED');
  }
  extraordinary(): FormArray<FormGroup<ExtraControls>> {
    return this.planForm.controls.extraordinary;
  }
  addExtra(): void { this.extraordinary().push(this.extraGroup()); }
  removeExtra(index: number): void { this.extraordinary().removeAt(index); }
  regularBalance(): number {
    const value = this.planForm.getRawValue();
    const extras = value.extraordinary.reduce((sum, row) => sum + (Number(row.amount) || 0), 0);
    const total = Number(value.totalPrice) || 0;
    const down = Number(value.downPayment) || 0;
    const delivery = Number(value.deliveryAmount) || 0;
    return Math.round((total - down - delivery - extras) * 100) / 100;
  }
  reload(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.api.contract(id).subscribe((detail) => {
      this.contract.set(detail.contract);
      this.parties.set(detail.parties);
      this.api.plan(id).subscribe({
        next: (plan) => {
          this.plan.set(plan);
          this.fillPlan(detail.contract, plan);
        },
        error: () => this.fillPlan(detail.contract, { plan: null, installments: [], payments: [], allocations: [], paid: 0, balance: detail.contract.agreedPrice }),
      });
    });
    this.api.documents('CONTRACT', id).subscribe((documents) => this.documents.set(documents));
  }
  addParty(): void { const id = this.contract()?.id; if (id && this.customerId()) this.api.addParty(id, this.customerId(), this.parties().length === 0).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) }); }
  removeParty(party: ContractParty): void { const id = this.contract()?.id; if (id) this.api.removeParty(id, party.customerId).subscribe({ next: () => this.reload() }); }
  savePlan(): void {
    const id = this.contract()?.id;
    if (!id || this.planLocked()) return;
    const value = this.planForm.getRawValue();
    const extraordinary = value.extraordinary.map((row) => ({ label: row.label.trim(), dueDate: row.dueDate, amount: Number(row.amount) }));
    if (extraordinary.some((row) => !row.label || !row.dueDate || !(row.amount > 0))) {
      this.extraordinary().markAllAsTouched();
      this.message.set('Cada cuota extraordinaria necesita concepto, fecha y un monto mayor que cero.');
      return;
    }
    this.api.savePlan(id, {
      totalPrice: Number(value.totalPrice),
      downPayment: Number(value.downPayment),
      downPaymentDueDate: value.downPaymentDueDate || null,
      installmentCount: Number(value.installmentCount),
      frequency: value.frequency,
      firstDueDate: value.firstDueDate || null,
      deliveryAmount: value.deliveryAmount ? Number(value.deliveryAmount) : null,
      deliveryDueDate: value.deliveryDueDate || null,
      conditions: value.conditions,
      extraordinary,
    }).subscribe({
      next: (plan) => {
        this.plan.set(plan);
        const current = this.contract();
        if (current) this.fillPlan(current, plan);
        this.message.set('Calendario generado.');
      },
      error: (error) => this.message.set(problemMessage(error)),
    });
  }
  register(): void {
    const id = this.contract()?.id; if (!id) return;
    const value = this.paymentForm.getRawValue();
    this.api.registerPayment(id, Number(value.amount), value.reference, value.notes, this.receipt()).subscribe({ next: () => { this.message.set('Pago confirmado y aplicado al calendario.'); this.reload(); }, error: (error) => this.message.set(problemMessage(error)) });
  }
  validate(payment: Payment): void { this.api.validatePayment(payment.id).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) }); }
  reject(payment: Payment): void { const reason = prompt('Motivo del rechazo') ?? ''; if (!reason.trim()) return; this.api.rejectPayment(payment.id, reason).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) }); }
  reverse(payment: Payment): void { const reason = prompt('Motivo de la reversión') ?? ''; if (!reason.trim() || !confirm('El pago confirmado se reversa y las cuotas se recalculan.')) return; this.api.reversePayment(payment.id, reason).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) }); }
  activate(): void { const id = this.contract()?.id; if (id && confirm('Activar el contrato marca la unidad como vendida. La reserva no se convierte sola.')) this.api.activateContract(id).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) }); }
  complete(): void { const id = this.contract()?.id; if (id) this.api.completeContract(id).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) }); }
  annul(): void { const id = this.contract()?.id; const reason = prompt('Motivo de la anulación') ?? ''; if (!id || !reason.trim()) return; this.api.annulContract(id, reason).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) }); }
  uploadDoc(event: Event): void { const id = this.contract()?.id; const file = (event.target as HTMLInputElement).files?.[0]; if (!id || !file) return; this.api.uploadDocument('CONTRACT', id, this.visible() ? 'BUYER_VISIBLE' : 'PRIVATE', file).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) }); }
  download(document: DocumentItem): void { this.api.downloadDocument(document.id, document.fileName).subscribe({ error: (error) => this.message.set(problemMessage(error)) }); }
  print(): void { window.print(); }
  customerName(id: string) { return this.customers().find((item) => item.id === id)?.fullName ?? 'Comprador'; }
  status(value: string) { return label(CONTRACT_STATUS, value); }
  installment(value: string) { return label(INSTALLMENT_STATUS, value); }
  kind(value: string) { return label(INSTALLMENT_KIND, value); }
  private fillPlan(contract: Contract, view: PlanView): void {
    const rows = view.installments;
    const header = view.plan;
    const down = rows.find((row) => row.kind === 'DOWN_PAYMENT');
    const firstRegular = rows.find((row) => row.kind === 'REGULAR');
    const delivery = rows.find((row) => row.kind === 'DELIVERY');
    this.planForm.patchValue({
      totalPrice: header?.totalPrice ?? contract.agreedPrice,
      downPayment: header?.downPayment ?? contract.agreedDownPayment,
      downPaymentDueDate: down?.dueDate ?? '',
      installmentCount: header?.installmentCount ?? 12,
      frequency: header?.frequency || 'MONTHLY',
      firstDueDate: firstRegular?.dueDate ?? '',
      deliveryAmount: header?.deliveryAmount == null ? '' : String(header.deliveryAmount),
      deliveryDueDate: delivery?.dueDate ?? '',
      conditions: header?.conditions ?? '',
    });
    const extras = this.extraordinary();
    extras.clear();
    for (const row of rows.filter((item) => item.kind === 'EXTRAORDINARY')) {
      extras.push(this.extraGroup(row.label, row.dueDate, Number(row.amount)));
    }
  }
  private extraGroup(labelText = '', dueDate = '', amount = 0): FormGroup<ExtraControls> {
    return this.formBuilder.nonNullable.group({
      label: [labelText, [Validators.required, Validators.maxLength(120)]],
      dueDate: [dueDate, Validators.required],
      amount: [amount, [Validators.required, Validators.min(0.01)]],
    });
  }
  paymentStatus(value: string) { return label(PAYMENT_STATUS, value); }
  selectValue(event: Event) { return (event.target as HTMLSelectElement).value; }
  fileOf(event: Event) { return (event.target as HTMLInputElement).files?.[0] ?? null; }
  checked(event: Event) { return (event.target as HTMLInputElement).checked; }
  readonly money = money; readonly when = when;
}

@Component({
  selector: 'app-payments',
  imports: [Loading, EmptyState, Pager],
  template: `
    <h1 class="text-4xl">Pagos</h1>
    <div class="mt-3 flex gap-2 text-sm">@for (item of filters; track item[0]) { <button type="button" class="rounded-full border border-line px-3 py-1" (click)="filter.set(item[0]); load(0)">{{ item[1] }}</button> }</div>
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin pagos en este filtro" detail="Los comprobantes reportados por compradores aparecen como reportados hasta que finanzas los confirme." /></div> }
    @else {
      <div class="mt-4 overflow-x-auto rounded-2xl border border-line bg-paper"><table class="w-full min-w-[680px] text-left text-sm"><thead class="text-ink-soft"><tr><th class="p-3">Fecha</th><th>Monto</th><th>Estado</th><th>Referencia</th><th></th></tr></thead>
        <tbody>@for (item of items(); track item.id) { <tr class="border-t border-line"><td class="p-3">{{ when(item.createdAt, true) }}</td><td>{{ money(item.amount, item.currency) }}</td><td>{{ status(item.status) }}</td><td>{{ item.referenceNumber || '—' }}</td><td class="space-x-2">@if (item.status === 'REPORTED') { <button type="button" (click)="validate(item)">Confirmar</button><button type="button" (click)="reject(item)">Rechazar</button> }</td></tr> }</tbody></table></div>
      <app-pager [page]="page()" [pages]="pages()" [total]="total()" (pageChange)="load($event)" />
    }
    @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
  `,
})
export class PaymentsPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<Payment[]>([]);
  readonly loading = signal(true);
  readonly message = signal('');
  readonly filter = signal('');
  readonly page = signal(0); readonly pages = signal(0); readonly total = signal(0);
  readonly filters: [string, string][] = [['', 'Todos'], ['REPORTED', 'Reportados'], ['VALIDATED', 'Confirmados'], ['REJECTED', 'Rechazados'], ['REVERSED', 'Reversados']];
  ngOnInit(): void { this.load(0); }
  load(page: number): void { this.api.payments({ status: this.filter() || null, page, size: 20 }).subscribe({ next: (result) => { this.items.set(result.content); this.page.set(result.page); this.pages.set(result.totalPages); this.total.set(result.totalElements); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  validate(item: Payment): void { this.api.validatePayment(item.id).subscribe({ next: () => this.load(this.page()), error: (error) => this.message.set(problemMessage(error)) }); }
  reject(item: Payment): void { const reason = prompt('Motivo del rechazo') ?? ''; if (!reason.trim()) return; this.api.rejectPayment(item.id, reason).subscribe({ next: () => this.load(this.page()), error: (error) => this.message.set(problemMessage(error)) }); }
  status(value: string) { return label(PAYMENT_STATUS, value); }
  readonly money = money; readonly when = when;
}

@Component({
  selector: 'app-team',
  imports: [ReactiveFormsModule, Loading],
  template: `
    <h1 class="text-4xl">Equipo</h1>
    <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-4 md:grid-cols-3" [formGroup]="form" (ngSubmit)="add()">
      <label class="text-sm">Nombre<input class="field" formControlName="fullName" /></label>
      <label class="text-sm">Correo<input class="field" formControlName="email" /></label>
      <label class="text-sm">Teléfono<input class="field" formControlName="phone" /></label>
      <label class="text-sm">Rol<select class="field" formControlName="role"><option value="COMPANY_ADMIN">Administrador</option><option value="SALES_ADVISOR">Asesor comercial</option><option value="FINANCE">Finanzas</option></select></label>
      <label class="text-sm md:col-span-2">Contraseña inicial<input class="field" type="password" formControlName="password" /></label>
      <button class="rounded-full bg-sea px-4 py-2 text-sand" type="submit">Invitar</button>
    </form>
    @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
    @if (loading()) { <app-loading /> }
    <ul class="mt-4 space-y-2">@for (member of members(); track member.id) {
      <li class="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-line bg-paper p-3 text-sm">
        <span>{{ member.fullName }} · {{ member.email }} · {{ role(member.role) }} · {{ member.status === 'ACTIVE' ? 'Activo' : 'Desactivado' }}</span>
        <button type="button" (click)="toggle(member)">{{ member.status === 'ACTIVE' ? 'Desactivar' : 'Activar' }}</button>
      </li>
    }</ul>
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class TeamPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly formBuilder = inject(FormBuilder);
  readonly members = signal<Member[]>([]);
  readonly loading = signal(true);
  readonly message = signal('');
  readonly form = this.formBuilder.nonNullable.group({ fullName: ['', Validators.required], email: ['', [Validators.required, Validators.email]], phone: [''], role: ['SALES_ADVISOR', Validators.required], password: ['', [Validators.required, Validators.minLength(10)]] });
  ngOnInit(): void { this.reload(); }
  reload(): void { this.api.members().subscribe({ next: (members) => { this.members.set(members); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  add(): void { this.api.addMember(this.form.getRawValue()).subscribe({ next: () => { this.form.reset({ role: 'SALES_ADVISOR' }); this.message.set('Usuario agregado.'); this.reload(); }, error: (error) => this.message.set(problemMessage(error)) }); }
  toggle(member: Member): void {
    const status = member.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    if (status === 'DISABLED' && !confirm('¿Desactivar este acceso?')) return;
    this.api.updateMember(member.id, { role: member.role, status }).subscribe({ next: () => this.reload(), error: (error) => this.message.set(problemMessage(error)) });
  }
  role(value: string) { return label(ROLE_LABELS, value); }
}

@Component({
  selector: 'app-templates',
  imports: [Loading],
  template: `
    <h1 class="text-4xl">Plantillas de aviso</h1>
    <p class="mt-2 text-sm text-ink-soft">El correo usa la plantilla de la empresa. Si no hay una propia, se usa la plantilla global. WhatsApp queda preparado, pero no se envía en esta versión.</p>
    @if (loading()) { <app-loading /> }
    <div class="mt-4 space-y-4">@for (item of items(); track item.code) {
      <form class="rounded-2xl border border-line bg-paper p-4" (submit)="save(item, $event)">
        <p class="text-sm text-ink-soft">{{ item.description }} {{ item.customized ? '· personalizada' : '· global' }}</p>
        <input class="field mt-2" name="subject" [value]="item.subject" />
        <textarea class="field mt-2" name="body" rows="4">{{ item.body }}</textarea>
        <label class="mt-2 flex items-center gap-2 text-sm"><input type="checkbox" name="enabled" [checked]="item.enabled !== false" /> Enviar este aviso</label>
        <button class="mt-2 rounded-full bg-sea px-3 py-1 text-sm text-sand" type="submit">Guardar</button>
      </form>
    }</div>
    @if (message()) { <p class="mt-2 text-sm">{{ message() }}</p> }
  `,
  styles: `.field { width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class TemplatesPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<TemplateView[]>([]);
  readonly loading = signal(true);
  readonly message = signal('');
  ngOnInit(): void { this.api.companyTemplates().subscribe({ next: (items) => { this.items.set(items); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  save(item: TemplateView, event: Event): void {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const data = new FormData(form);
    this.api.saveCompanyTemplate(item.code, String(data.get('subject') ?? ''), String(data.get('body') ?? ''), data.get('enabled') === 'on').subscribe({ next: () => this.message.set('Plantilla guardada.'), error: (error) => this.message.set(problemMessage(error)) });
  }
}

@Component({
  selector: 'app-audit',
  imports: [Loading, EmptyState, Pager],
  template: `
    <h1 class="text-4xl">Auditoría</h1>
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin eventos" detail="Los cambios sensibles de la empresa se registran aquí." /></div> }
    @else { <ul class="mt-4 space-y-2">@for (item of items(); track item.id) { <li class="rounded-2xl bg-paper p-3 text-sm"><strong>{{ item.action }}</strong> · {{ when(item.createdAt, true) }}<br />{{ item.summary }}</li> }</ul><app-pager [page]="page()" [pages]="pages()" [total]="total()" (pageChange)="load($event)" /> }
  `,
})
export class AuditPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<AuditEvent[]>([]);
  readonly loading = signal(true);
  readonly page = signal(0); readonly pages = signal(0); readonly total = signal(0);
  ngOnInit(): void { this.load(0); }
  load(page: number): void { this.api.audit(page).subscribe({ next: (result) => { this.items.set(result.content); this.page.set(result.page); this.pages.set(result.totalPages); this.total.set(result.totalElements); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  readonly when = when;
}

@Component({
  selector: 'app-notices',
  imports: [Loading, EmptyState],
  template: `
    <div class="flex items-center justify-between"><h1 class="text-4xl">Avisos</h1><button type="button" class="text-sm" (click)="readAll()">Marcar todos como leídos</button></div>
    @if (loading()) { <app-loading /> }
    @else if (items().length === 0) { <div class="mt-4"><app-empty title="Sin avisos" detail="Las reservas, cuotas y comprobantes generan notificaciones para las personas autorizadas." /></div> }
    @else { <ul class="mt-4 space-y-2">@for (item of items(); track item.id) { <li class="rounded-2xl border border-line bg-paper p-3" [class.opacity-60]="item.readAt"><button type="button" class="text-left" (click)="read(item)"><strong>{{ item.title }}</strong><br /><span class="text-sm">{{ item.body }}</span><br /><span class="text-xs text-ink-soft">{{ when(item.createdAt, true) }}</span></button></li> }</ul> }
  `,
})
export class NoticesPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly items = signal<Notice[]>([]);
  readonly loading = signal(true);
  ngOnInit(): void { this.api.notifications().subscribe({ next: (page) => { this.items.set(page.content); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  read(item: Notice): void { if (!item.readAt) this.api.markRead(item.id).subscribe(() => this.items.update((items) => items.map((notice) => notice.id === item.id ? { ...notice, readAt: new Date().toISOString() } : notice))); }
  readAll(): void { this.api.markAllRead().subscribe(() => this.items.update((items) => items.map((notice) => ({ ...notice, readAt: notice.readAt ?? new Date().toISOString() })))); }
  readonly when = when;
}
