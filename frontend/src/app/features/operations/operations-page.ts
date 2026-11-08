import { Component, computed, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';

import { Fund, Order, ProcessingResult } from '../../core/models';
import { SAMPLE_FUNDS, SAMPLE_ORDERS } from '../../core/sample-data';

@Component({
  selector: 'app-operations-page',
  imports: [CurrencyPipe, DatePipe, DecimalPipe],
  templateUrl: './operations-page.html',
  styleUrl: './operations-page.css',
})
export class OperationsPage {
  protected readonly today = new Date();

  // TODO (you): load from the API
  //   FundService.getFunds()                          → GET /api/funds
  //   OrderService.getOrders('PENDING')               → GET /api/orders?status=PENDING
  protected readonly funds = signal<Fund[]>(SAMPLE_FUNDS);
  protected readonly pendingOrders = signal<Order[]>(
    SAMPLE_ORDERS.filter((o) => o.status === 'PENDING'),
  );

  // TODO (you): set this from the response of "Process day"
  protected readonly lastResult = signal<ProcessingResult | null>({
    date: '2026-11-07',
    settled: 5,
    rejected: 1,
    stillPending: 0,
  });

  protected readonly pendingCount = computed(() => this.pendingOrders().length);

  // TODO (you):
  //   saveNav(fund, value)  → FundService.addNav(fund.id, { priceDate: today, nav })
  //                           → POST /api/funds/{id}/nav   (handle 409 = "NAV already entered today")
  //   processDay()          → OrderService.processDay(today) → POST /api/orders/process?date=YYYY-MM-DD
  //                           → put the result in lastResult, then reload pending orders
}
