import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { toIsoDate } from '../../core/dates';
import { FundService } from '../../core/fund.service';
import { Fund, NavPrice, Order, ProcessingResult } from '../../core/models';
import { OrderService } from '../../core/order.service';
import { maxDecimals } from '../../core/validators';

@Component({
  selector: 'app-operations-page',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, ReactiveFormsModule],
  templateUrl: './operations-page.html',
  styleUrl: './operations-page.css',
})
export class OperationsPage implements OnInit {
  private readonly fundService = inject(FundService);
  private readonly orderService = inject(OrderService);

  // ── A. The business date being worked on (defaults to today) ─────────────
  protected readonly maxDate = toIsoDate(new Date()); // no future dates (backend: @PastOrPresent)
  protected readonly selectedDate = signal(this.maxDate);

  protected readonly funds = signal<Fund[]>([]);
  protected readonly pendingOrders = signal<Order[]>([]); // ALL pending orders, any date
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  // Pending orders split by the selected date: Process day only prices that date's orders
  protected readonly pendingForDate = computed(() =>
    this.pendingOrders().filter((o) => o.orderDate === this.selectedDate()),
  );
  protected readonly pendingOtherDates = computed(
    () => this.pendingOrders().length - this.pendingForDate().length,
  );
  protected readonly pendingCount = computed(() => this.pendingForDate().length);

  // ── B. NAV entry (one input per fund) ──────────────────────────────────────
  protected readonly navsForDate = signal<Record<number, NavPrice>>({}); // fundId → NAV already saved
  protected navInputs: Record<number, FormControl<number | null>> = {}; // fundId → its input
  protected readonly savingFundId = signal<number | null>(null);
  protected readonly navErrors = signal<Record<number, string>>({}); // fundId → error message

  // ── C. Process day ─────────────────────────────────────────────────────────
  protected readonly lastResult = signal<ProcessingResult | null>(null);
  protected readonly processing = signal(false);
  protected readonly processError = signal<string | null>(null);

  // Funds with no NAV for the date: their orders will stay PENDING (warn before running)
  protected readonly fundsMissingNav = computed(() =>
    this.funds().filter((f) => !this.navsForDate()[f.id]),
  );

  ngOnInit(): void {
    forkJoin({
      funds: this.fundService.getFunds(),
      pending: this.orderService.getOrders({ status: 'PENDING' }),
    }).subscribe({
      next: ({ funds, pending }) => {
        this.funds.set(funds);
        this.pendingOrders.set(pending);
        this.navInputs = Object.fromEntries(funds.map((f) => [f.id, this.newNavControl()]));
        this.loadNavs();
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set('Could not load funds and orders. Is the backend running on port 8080?');
        this.loading.set(false);
      },
    });
  }

  protected onDateChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value; // "YYYY-MM-DD", or "" if cleared
    if (!value) return;
    this.selectedDate.set(value); // pendingForDate re-filters automatically (computed)
    this.navErrors.set({});
    this.lastResult.set(null);
    this.loadNavs();
  }

  // Which funds already have a NAV for the selected date? (one small GET per fund, in parallel)
  private loadNavs(): void {
    const date = this.selectedDate();
    const requests = Object.fromEntries(
      this.funds().map((f) => [f.id, this.fundService.getNavs(f.id, date)]),
    );
    forkJoin(requests).subscribe({
      next: (navListsByFund) => {
        const saved: Record<number, NavPrice> = {};
        for (const [fundId, navs] of Object.entries(navListsByFund)) {
          if (navs.length > 0) saved[Number(fundId)] = navs[0];
        }
        this.navsForDate.set(saved);
      },
      error: () => this.loadError.set('Could not load NAVs for this date.'),
    });
  }

  protected saveNav(fund: Fund): void {
    if (this.savingFundId() !== null) return; // one save at a time

    const control = this.navInputs[fund.id];
    if (control.invalid) {
      control.markAsTouched();
      return;
    }

    this.savingFundId.set(fund.id);
    this.setNavError(fund.id, null);

    this.fundService.addNav(fund.id, { priceDate: this.selectedDate(), nav: control.value! }).subscribe({
      next: (saved) => {
        this.navsForDate.update((all) => ({ ...all, [fund.id]: saved }));
        control.reset();
        this.savingFundId.set(null);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 409) {
          this.setNavError(fund.id, 'A NAV was already entered for this date.');
          this.loadNavs(); // show the existing one
        } else {
          this.setNavError(fund.id, 'Could not save. Check the value and try again.');
        }
        this.savingFundId.set(null);
      },
    });
  }

  private newNavControl(): FormControl<number | null> {
    // Same rules as the backend's CreateNavPriceRequest: @NotNull @Positive @Digits(fraction = 4)
    return new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(0.0001),
      maxDecimals(4),
    ]);
  }

  private setNavError(fundId: number, message: string | null): void {
    this.navErrors.update((all) => {
      const next = { ...all };
      if (message) next[fundId] = message;
      else delete next[fundId];
      return next;
    });
  }

  protected processDay(): void {
    if (this.processing()) return; // already running: ignore extra clicks

    this.processing.set(true);
    this.processError.set(null);

    this.orderService.processDay(this.selectedDate()).subscribe({
      next: (result) => {
        this.lastResult.set(result); // { date, settled, rejected, stillPending }
        this.processing.set(false);
        this.reloadPending(); // settled/rejected orders drop off the pending list
      },
      error: () => {
        // The backend's @Transactional rolled back the whole run, so nothing was half-done
        this.processError.set('Processing failed. No orders were changed. Try again.');
        this.processing.set(false);
      },
    });
  }

  private reloadPending(): void {
    this.orderService.getOrders({ status: 'PENDING' }).subscribe({
      next: (pending) => this.pendingOrders.set(pending),
    });
  }
}
