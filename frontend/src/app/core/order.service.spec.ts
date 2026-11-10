import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { Order, ProcessingResult } from './models';
import { OrderService } from './order.service';

// HttpTestingController = a fake backend (like Mockito for HTTP):
//   expectOne(url) → assert exactly one request was made to that URL (like verify(...))
//   req.flush(data) → answer it with fake data              (like when(...).thenReturn(...))
describe('OrderService', () => {
  let service: OrderService;
  let http: HttpTestingController;

  const anOrder: Order = {
    id: 7,
    clientId: 3,
    clientName: 'Maria Lopez',
    fundId: 1,
    ticker: 'MPL100',
    orderType: 'BUY',
    amount: 500,
    units: null,
    orderDate: '2026-11-10',
    status: 'PENDING',
    priceUsed: null,
    unitsSettled: null,
    createdAt: '2026-11-10T10:15:00',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()], // swap the real HTTP for the fake
    });
    service = TestBed.inject(OrderService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify(); // fail if any request was made that a test didn't expect
  });

  it('getOrders() with no filters calls GET /api/orders', () => {
    let result: Order[] = [];
    service.getOrders().subscribe((orders) => (result = orders));

    const req = http.expectOne('/api/orders');
    expect(req.request.method).toBe('GET');
    req.flush([anOrder]);

    expect(result).toEqual([anOrder]);
  });

  it('getOrders({ clientId }) adds ?clientId=', () => {
    service.getOrders({ clientId: 3 }).subscribe();

    const req = http.expectOne('/api/orders?clientId=3');
    expect(req.request.params.has('status')).toBe(false); // filters not given are left out
    req.flush([]);
  });

  it('getOrders({ status }) adds ?status=', () => {
    service.getOrders({ status: 'PENDING' }).subscribe();

    http.expectOne('/api/orders?status=PENDING').flush([]);
  });

  it('getOrders() with both filters sends both', () => {
    service.getOrders({ clientId: 3, status: 'SETTLED' }).subscribe();

    http.expectOne('/api/orders?clientId=3&status=SETTLED').flush([]);
  });

  it('placeOrder() POSTs the request body and returns the new order', () => {
    let placed: Order | undefined;
    const request = { clientId: 3, fundId: 1, orderType: 'BUY' as const, amount: 500 };

    service.placeOrder(request).subscribe((order) => (placed = order));

    const req = http.expectOne('/api/orders');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request); // only amount: no units key for a BUY
    req.flush(anOrder, { status: 201, statusText: 'Created' });

    expect(placed?.id).toBe(7);
    expect(placed?.status).toBe('PENDING');
  });

  it('placeOrder() reports a 400 as an error', () => {
    let status = 0;
    service
      .placeOrder({ clientId: 999, fundId: 1, orderType: 'BUY', amount: 500 })
      .subscribe({ error: (err) => (status = err.status) });

    http.expectOne('/api/orders').flush(null, { status: 400, statusText: 'Bad Request' });

    expect(status).toBe(400);
  });

  it('processDay() POSTs to /api/orders/process with the date as a query param and no body', () => {
    const summary: ProcessingResult = { date: '2026-11-10', settled: 2, rejected: 1, stillPending: 0 };
    let result: ProcessingResult | undefined;

    service.processDay('2026-11-10').subscribe((r) => (result = r));

    const req = http.expectOne('/api/orders/process?date=2026-11-10');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(summary);

    expect(result).toEqual(summary);
  });
});
