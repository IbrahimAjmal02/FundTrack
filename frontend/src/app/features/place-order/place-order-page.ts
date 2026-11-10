import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ClientService } from '../../core/client.service';
import { FundService } from '../../core/fund.service';
import { Client, Fund, Order, OrderType, PlaceOrderRequest } from '../../core/models';
import { OrderService } from '../../core/order.service';
import { maxDecimals } from '../../core/validators';

@Component({
  selector: 'app-place-order-page',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './place-order-page.html',
})
export class PlaceOrderPage implements OnInit {
  private readonly clientService = inject(ClientService);
  private readonly fundService = inject(FundService);
  private readonly orderService = inject(OrderService);

  // Options for the two dropdowns
  protected readonly clients = signal<Client[]>([]);
  protected readonly funds = signal<Fund[]>([]);
  protected readonly loadError = signal<string | null>(null);

  ngOnInit(): void {
    forkJoin({
      clients: this.clientService.getClients(),
      funds: this.fundService.getFunds(),
    }).subscribe({
      next: ({ clients, funds }) => {
        this.clients.set(clients);
        this.funds.set(funds);
      },
      error: () => this.loadError.set('Could not load clients and funds. Is the backend running on port 8080?'),
    });
  }

  // UI state for the BUY / SELL toggle
  protected readonly orderType = signal<OrderType>('BUY');
  protected readonly today = new Date();

  // The order form. amount/units start with NO validators: applyRulesFor() adds them
  // to whichever field the current order type uses.
  protected readonly form = new FormGroup({
    clientId: new FormControl<number | null>(null, Validators.required),
    fundId: new FormControl<number | null>(null, Validators.required),
    amount: new FormControl<number | null>(null), // BUY: dollars
    units: new FormControl<number | null>(null), // SELL: units
  });

  protected readonly submitting = signal(false); // true while the POST is in flight
  protected readonly submitError = signal<string | null>(null);
  protected readonly placedOrder = signal<Order | null>(null); // the last order placed (201 body)

  constructor() {
    this.applyRulesFor(this.orderType()); // rules for the starting order type
  }

  protected setType(type: OrderType): void {
    this.orderType.set(type);
    this.applyRulesFor(type);
  }

  // Frontend version of @AssertTrue isAmountOrUnitsMatchingOrderType() in PlaceOrderRequest.java:
  //   BUY  → amount required (> 0, max 2 decimals), units unused
  //   SELL → units required (> 0, max 4 decimals), amount unused
  private applyRulesFor(type: OrderType): void {
    const { amount, units } = this.form.controls;

    if (type === 'BUY') {
      amount.setValidators([Validators.required, Validators.min(0.01), maxDecimals(2)]);
      units.clearValidators();
      units.reset(); // clear anything typed before switching
    } else {
      units.setValidators([Validators.required, Validators.min(0.0001), maxDecimals(4)]);
      amount.clearValidators();
      amount.reset();
    }

    // setValidators only swaps the rules; this re-checks the fields against the new rules
    amount.updateValueAndValidity();
    units.updateValueAndValidity();
  }

  submit(): void {
    if (this.submitting()) return; // already sending: ignore extra clicks

    if (this.form.invalid) {
      this.form.markAllAsTouched(); // show every field's error message
      return;
    }

    const { clientId, fundId, amount, units } = this.form.getRawValue();
    const type = this.orderType();

    // Send ONLY the field this order type uses (the backend's @AssertTrue rejects both)
    const request: PlaceOrderRequest = {
      clientId: clientId!, // "!" = "not null": the required validator already guaranteed it
      fundId: fundId!,
      orderType: type,
      ...(type === 'BUY' ? { amount: amount! } : { units: units! }),
    };

    this.submitting.set(true);
    this.submitError.set(null);
    this.placedOrder.set(null);

    this.orderService.placeOrder(request).subscribe({
      next: (order) => {
        this.placedOrder.set(order); // shows the "Order #… placed" message
        this.form.reset();
        this.submitting.set(false);
      },
      error: () => {
        this.submitError.set('The order could not be placed. Check the details and try again.');
        this.submitting.set(false);
      },
    });
  }
}
