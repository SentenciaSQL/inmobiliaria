import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink, RouterOutlet } from '@angular/router';
import { ApiService } from '../core/api.service';
import { INSTALLMENT_KIND, INSTALLMENT_STATUS, PAYMENT_STATUS, label } from '../core/labels';
import { BuyerHome, BuyerOperation, DocumentItem, Notice } from '../core/models';
import { SessionService } from '../core/session.service';
import { EmptyState, Loading, Logo, money, problemMessage, when } from '../shared/ui';

@Component({
  selector: 'app-buyer-shell',
  imports: [RouterOutlet, RouterLink, Logo],
  template: `
    <header class="border-b border-line bg-paper">
      <div class="mx-auto flex max-w-5xl items-center justify-between gap-3 px-4 py-4">
        <a routerLink="/portal" class="flex items-center gap-3">
          <span class="h-10 w-10 overflow-hidden rounded-xl"><app-logo [src]="home()?.logoUrl ?? null" [name]="home()?.companyName || 'Constructora'" [alt]="home()?.companyName || 'Logo'" /></span>
          <span class="font-display text-xl">{{ home()?.companyName || 'Mi operación' }}</span>
        </a>
        <div class="flex gap-3 text-sm">
          @if ((session.user()?.memberships?.length ?? 0) > 1) { <a routerLink="/seleccionar-empresa">Otra empresa</a> }
          <a routerLink="/cuenta">Contraseña</a>
          <button type="button" (click)="session.logout()">Salir</button>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-5xl px-4 py-6"><router-outlet /></main>
  `,
})
export class BuyerShell implements OnInit {
  readonly session = inject(SessionService);
  private readonly api = inject(ApiService);
  readonly home = signal<BuyerHome | null>(null);
  ngOnInit(): void { this.api.buyerSummary().subscribe((home) => this.home.set(home)); }
}

@Component({
  selector: 'app-buyer-home',
  imports: [RouterLink, Loading, EmptyState, Logo],
  template: `
    @if (loading()) { <app-loading /> }
    @else if (home(); as summary) {
      <h1 class="text-4xl">Hola{{ summary.customerName ? ', ' + summary.customerName : '' }}</h1>
      <p class="mt-2 text-sm text-ink-soft">Aquí solo aparecen las operaciones de {{ summary.companyName }} vinculadas a su usuario.</p>
      @if (summary.operations.length === 0) { <div class="mt-4"><app-empty title="Todavía no hay una operación" detail="Cuando la constructora active su contrato, el calendario de cuotas aparecerá aquí." /></div> }
      <div class="mt-6 grid gap-4">
        @for (item of summary.operations; track item.contractId) {
          <a class="rounded-3xl border border-line bg-paper p-5" [routerLink]="['/portal/operaciones', item.contractId]">
            <div class="flex items-center gap-3"><span class="h-10 w-10 overflow-hidden rounded-xl"><app-logo [src]="summary.logoUrl" [name]="summary.companyName" [alt]="summary.companyName" /></span><span class="font-display text-2xl">{{ item.projectName }} · {{ item.unitCode }}</span></div>
            <p class="mt-2 text-sm">Precio {{ money(item.agreedPrice, item.currency) }} · Saldo {{ money(item.plan?.balance, item.currency) }}</p>
          </a>
        }
      </div>
      <section class="mt-8 text-sm">
        <h2 class="font-display text-2xl">Contacto</h2>
        @if (summary.publicPhone) { <a class="mr-4" [href]="'tel:' + summary.publicPhone">{{ summary.publicPhone }}</a> }
        @if (summary.publicEmail) { <a [href]="'mailto:' + summary.publicEmail">{{ summary.publicEmail }}</a> }
      </section>
    }
  `,
})
export class BuyerHomePage implements OnInit {
  private readonly api = inject(ApiService);
  readonly home = signal<BuyerHome | null>(null);
  readonly loading = signal(true);
  ngOnInit(): void { this.api.buyerSummary().subscribe({ next: (home) => { this.home.set(home); this.loading.set(false); }, error: () => this.loading.set(false) }); }
  readonly money = money;
}

@Component({
  selector: 'app-buyer-operation',
  imports: [ReactiveFormsModule, RouterLink, Loading],
  template: `
    <a class="text-sm text-sea" routerLink="/portal">Volver</a>
    @if (loading()) { <app-loading /> }
    @else if (operation(); as item) {
      <h1 class="mt-2 text-4xl">{{ item.projectName }} · {{ item.unitCode }}</h1>
      <p class="mt-2">Precio contractual {{ money(item.agreedPrice, item.currency) }}. Inicial acordada {{ money(item.agreedDownPayment, item.currency) }}.</p>
      <div class="mt-4 grid gap-3 sm:grid-cols-3">
        <article class="rounded-2xl bg-paper p-4"><p class="text-sm text-ink-soft">Pagado</p><p class="font-display text-2xl">{{ money(item.plan?.paid, item.currency) }}</p></article>
        <article class="rounded-2xl bg-paper p-4"><p class="text-sm text-ink-soft">Saldo</p><p class="font-display text-2xl">{{ money(item.plan?.balance, item.currency) }}</p></article>
        <article class="rounded-2xl bg-paper p-4"><p class="text-sm text-ink-soft">{{ dueLabel(item) }}</p><p class="font-display text-2xl">{{ nextDue(item) }}</p></article>
      </div>
      <h2 class="mt-8 text-2xl">Calendario</h2>
      <div class="mt-3 overflow-x-auto rounded-2xl border border-line bg-paper"><table class="w-full min-w-[640px] text-left text-sm"><thead class="text-ink-soft"><tr><th class="p-3">Tipo</th><th>Cuota</th><th>Vence</th><th>Monto</th><th>Pagado</th><th>Estado</th></tr></thead>
        <tbody>@for (row of item.plan?.installments ?? []; track row.id) { <tr class="border-t border-line"><td class="p-3">{{ kind(row.kind) }}</td><td>{{ row.label }}</td><td>{{ when(row.dueDate) }}</td><td>{{ money(row.amount, item.currency) }}</td><td>{{ money(row.paidAmount, item.currency) }}</td><td>{{ installment(row.status) }}</td></tr> }</tbody></table></div>
      <h2 class="mt-8 text-2xl">Pagos</h2>
      <ul class="mt-3 space-y-2 text-sm">@for (payment of item.plan?.payments ?? []; track payment.id) { <li class="rounded-2xl bg-paper p-3">{{ money(payment.amount, payment.currency) }} · {{ paymentStatus(payment.status) }} @if (payment.rejectionReason) { · {{ payment.rejectionReason }} }</li> }</ul>
      <form class="mt-4 grid gap-3 rounded-3xl border border-line bg-paper p-4" [formGroup]="form" (ngSubmit)="report()">
        <h2 class="text-2xl">Reportar un pago</h2>
        <p class="text-sm text-ink-soft">Subir el comprobante no marca la cuota como pagada. Finanzas debe confirmarlo.</p>
        <label class="text-sm">Monto<input class="field" type="number" formControlName="amount" /></label>
        <label class="text-sm">Referencia<input class="field" formControlName="reference" /></label>
        <label class="text-sm">Notas<textarea class="field" rows="2" formControlName="notes"></textarea></label>
        <label class="text-sm">Comprobante<input type="file" accept="image/png,image/jpeg,image/webp,application/pdf" (change)="file.set(fileOf($event))" /></label>
        @if (message()) { <p class="text-sm">{{ message() }}</p> }
        <button class="rounded-full bg-sea px-4 py-2 text-sand disabled:opacity-50" [disabled]="form.invalid || !file() || sending()" type="submit">{{ sending() ? 'Enviando…' : 'Enviar comprobante' }}</button>
      </form>
      <h2 class="mt-8 text-2xl">Documentos</h2>
      <ul class="mt-2 text-sm">@for (document of documents(); track document.id) { <li><button type="button" class="underline" (click)="download(document)">{{ document.fileName }}</button></li> }</ul>
      <h2 class="mt-8 text-2xl">Avisos</h2>
      <ul class="mt-2 space-y-2 text-sm">@for (notice of notices(); track notice.id) { <li class="rounded-2xl bg-paper p-3">{{ notice.title }}<br />{{ notice.body }}</li> }</ul>
    }
  `,
  styles: `.field { margin-top: 0.25rem; width: 100%; border: 1px solid var(--color-line); border-radius: 0.75rem; padding: 0.5rem 0.75rem; }`,
})
export class BuyerOperationPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  readonly operation = signal<BuyerOperation | null>(null);
  readonly documents = signal<DocumentItem[]>([]);
  readonly notices = signal<Notice[]>([]);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly message = signal('');
  readonly file = signal<File | null>(null);
  readonly form = this.formBuilder.nonNullable.group({ amount: [0, Validators.required], reference: [''], notes: [''] });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.api.buyerContract(id).subscribe({ next: (operation) => { this.operation.set(operation); this.loading.set(false); }, error: () => this.loading.set(false) });
    this.api.buyerDocuments().subscribe((documents) => this.documents.set(documents));
    this.api.notifications().subscribe((page) => this.notices.set(page.content));
  }

  report(): void {
    const item = this.operation();
    const file = this.file();
    if (!item || !file) return;
    const value = this.form.getRawValue();
    this.sending.set(true);
    this.api.reportPayment(item.contractId, Number(value.amount), value.reference, value.notes, file).subscribe({
      next: () => { this.sending.set(false); this.message.set('Comprobante enviado. Queda pendiente de confirmación.'); this.api.buyerContract(item.contractId).subscribe((operation) => this.operation.set(operation)); },
      error: (error) => { this.sending.set(false); this.message.set(problemMessage(error)); },
    });
  }

  nextDue(item: BuyerOperation): string {
    const row = this.openInstallment(item);
    return row ? when(row.dueDate) : 'Sin pendientes';
  }
  dueLabel(item: BuyerOperation): string {
    return this.openInstallment(item)?.status === 'OVERDUE' ? 'Cuota vencida' : 'Próximo vencimiento';
  }
  private openInstallment(item: BuyerOperation) {
    return item.plan?.installments.find((installment) => installment.status !== 'PAID' && installment.status !== 'CANCELLED');
  }
  installment(value: string) { return label(INSTALLMENT_STATUS, value); }
  kind(value: string) { return label(INSTALLMENT_KIND, value); }
  paymentStatus(value: string) { return label(PAYMENT_STATUS, value); }
  fileOf(event: Event) { return (event.target as HTMLInputElement).files?.[0] ?? null; }
  download(document: DocumentItem): void {
    this.api.downloadDocument(document.id, document.fileName).subscribe({ error: (error) => this.message.set(problemMessage(error)) });
  }
  readonly money = money;
  readonly when = when;
}
