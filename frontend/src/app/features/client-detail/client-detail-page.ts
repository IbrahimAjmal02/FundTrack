import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ClientService } from '../../core/client.service';
import { Client, Holding, Order } from '../../core/models';
import { OrderService } from '../../core/order.service';

@Component({
  selector: 'app-client-detail-page',
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe],
  templateUrl: './client-detail-page.html',
})
export class ClientDetailPage implements OnInit {
  private readonly clientService = inject(ClientService);
  private readonly orderService = inject(OrderService);

  // The ":id" from the URL /clients/:id arrives here (thanks to withComponentInputBinding)
  readonly id = input.required<string>();

  protected readonly client = signal<Client | null>(null); // null until loaded (or if not found)
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly holdings = signal<Holding[]>([]);
  protected readonly orders = signal<Order[]>([]);

  ngOnInit(): void {
    const clientId = Number(this.id()); // URL params are strings: "3" → 3

    // Send all 3 requests at once; wait until ALL have answered (like Promise.all)
    forkJoin({
      client: this.clientService.getClient(clientId),
      holdings: this.clientService.getHoldings(clientId),
      orders: this.orderService.getOrders({ clientId }),
    }).subscribe({
      next: ({ client, holdings, orders }) => {
        this.client.set(client);
        this.holdings.set(holdings);
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.notFound.set(true);
        } else {
          this.error.set('Could not load this client. Is the backend running on port 8080?');
        }
        this.loading.set(false);
      },
    });
  }

  // computed(): recalculates automatically whenever holdings/orders change (like React's useMemo)
  protected readonly totalValue = computed(() =>
    this.holdings().reduce((sum, h) => sum + (h.marketValue ?? 0), 0),
  );
  protected readonly pendingCount = computed(
    () => this.orders().filter((o) => o.status === 'PENDING').length,
  );
}
