import { Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import { Client, Fund, OrderType } from '../../core/models';
import { SAMPLE_CLIENTS, SAMPLE_FUNDS } from '../../core/sample-data';

@Component({
  selector: 'app-place-order-page',
  imports: [DatePipe],
  templateUrl: './place-order-page.html',
})
export class PlaceOrderPage {
  // TODO (you): load these from ClientService.getClients() and FundService.getFunds()
  protected readonly clients = signal<Client[]>(SAMPLE_CLIENTS);
  protected readonly funds = signal<Fund[]>(SAMPLE_FUNDS);

  // UI state for the BUY / SELL toggle (already works: try clicking it)
  protected readonly orderType = signal<OrderType>('BUY');
  protected readonly today = new Date();

  protected setType(type: OrderType): void {
    this.orderType.set(type);
  }

  // TODO (you): turn the form into a Reactive Form (or Signal Form):
  //   - clientId, fundId: required
  //   - BUY → amount required, > 0, max 2 decimals;  SELL → units required, > 0, max 4 decimals
  //     (the same cross-field rule as @AssertTrue in PlaceOrderRequest.java)
  //   On submit → OrderService.placeOrder() → POST /api/orders
  //   On 201 → show "Order #id placed, PENDING until today's NAV is processed"
  //   On 400 → show the error
}
