import { Component, computed, input, output, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

export function problemMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const detail = error.error?.detail;
    if (typeof detail === 'string' && detail.trim()) return detail;
    if (error.status === 0) return 'No hay conexión con el servidor';
  }
  return 'No se pudo completar la operación';
}

export function money(value: number | null | undefined, currency = 'DOP'): string {
  if (value === null || value === undefined) return '—';
  return new Intl.NumberFormat('es-DO', { style: 'currency', currency, maximumFractionDigits: 2 }).format(value);
}

export function when(value: string | null | undefined, withTime = false): string {
  if (!value) return '—';
  const date = new Date(value.includes('T') ? value : `${value}T00:00:00`);
  return new Intl.DateTimeFormat('es-DO', withTime
    ? { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Santo_Domingo' }
    : { dateStyle: 'medium', timeZone: 'America/Santo_Domingo' }).format(date);
}

@Component({
  selector: 'app-logo',
  template: `
    @if (src()) {
      <img [src]="src()!" [alt]="alt()" class="h-full w-full object-contain" />
    } @else {
      <span class="grid h-full w-full place-items-center bg-sea font-display text-sm text-sand">{{ mark() }}</span>
    }
  `,
})
export class Logo {
  readonly src = input<string | null>(null);
  readonly alt = input('Logo');
  readonly name = input('Alzada');
  readonly mark = computed(() => this.name().trim().slice(0, 1).toUpperCase() || 'A');
}

@Component({
  selector: 'app-empty',
  template: `
    <div class="rounded-2xl border border-dashed border-line bg-paper px-6 py-10 text-center">
      <p class="font-display text-2xl text-ink">{{ title() }}</p>
      <p class="mx-auto mt-2 max-w-md text-sm text-ink-soft">{{ detail() }}</p>
    </div>
  `,
})
export class EmptyState {
  readonly title = input('No hay información');
  readonly detail = input('Cuando existan registros aparecerán aquí.');
}

@Component({
  selector: 'app-pager',
  template: `
    <div class="mt-4 flex items-center justify-between text-sm text-ink-soft">
      <span>{{ total() }} registros</span>
      <div class="flex gap-2">
        <button type="button" class="rounded-full border border-line px-3 py-1 disabled:opacity-40" [disabled]="page() <= 0" (click)="pageChange.emit(page() - 1)">Anterior</button>
        <button type="button" class="rounded-full border border-line px-3 py-1 disabled:opacity-40" [disabled]="page() + 1 >= pages()" (click)="pageChange.emit(page() + 1)">Siguiente</button>
      </div>
    </div>
  `,
})
export class Pager {
  readonly page = input(0);
  readonly pages = input(0);
  readonly total = input(0);
  readonly pageChange = output<number>();
}

@Component({
  selector: 'app-toast',
  template: `
    <div class="pointer-events-none fixed bottom-4 right-4 z-50 flex w-full max-w-sm flex-col gap-2" aria-live="polite">
      @for (item of items(); track item.id) {
        <div class="pointer-events-auto rounded-2xl px-4 py-3 text-sm shadow-lg" [class]="item.kind === 'error' ? 'bg-danger text-white' : 'bg-ink text-sand'">
          {{ item.text }}
        </div>
      }
    </div>
  `,
})
export class ToastHost {
  readonly items = signal<{ id: number; text: string; kind: 'ok' | 'error' }[]>([]);
  private next = 1;

  push(text: string, kind: 'ok' | 'error' = 'ok'): void {
    const id = this.next++;
    this.items.update((items) => [...items, { id, text, kind }]);
    setTimeout(() => this.items.update((items) => items.filter((item) => item.id !== id)), 4200);
  }
}

export const toast = {
  host: null as ToastHost | null,
};

@Component({
  selector: 'app-loading',
  template: `<p class="py-8 text-center text-sm text-ink-soft">Cargando…</p>`,
})
export class Loading {}
