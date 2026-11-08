import { Component, computed, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Client, Holding, Order } from '../../core/models';
import { SAMPLE_CLIENTS, SAMPLE_HOLDINGS, SAMPLE_ORDERS } from '../../core/sample-data';

@Component({
  selector: 'app-client-detail-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe],
  templateUrl: './client-detail-page.html',
})
export class ClientDetailPage {
  // The ":id" from the URL /clients/:id arrives here (thanks to withComponentInputBinding)
  readonly id = input.required<string>();

  // TODO (you): load these from the API using this.id():
  //   GET /api/clients/{id}            → client      (404 → show "client not found")
  //   GET /api/clients/{id}/holdings   → holdings
  //   GET /api/orders  (then keep only this client's orders) → orders
  protected readonly client = signal<Client>(SAMPLE_CLIENTS[0]);
  protected readonly holdings = signal<Holding[]>(SAMPLE_HOLDINGS);
  protected readonly orders = signal<Order[]>(SAMPLE_ORDERS.filter((o) => o.clientId === 1));

  // computed(): recalculates automatically whenever holdings/orders change (like React's useMemo)
  protected readonly totalValue = computed(() =>
    this.holdings().reduce((sum, h) => sum + (h.marketValue ?? 0), 0),
  );
  protected readonly pendingCount = computed(
    () => this.orders().filter((o) => o.status === 'PENDING').length,
  );
}
