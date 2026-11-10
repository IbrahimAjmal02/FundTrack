import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ClientService } from './client.service';
import { Client, Holding } from './models';

describe('ClientService', () => {
  let service: ClientService;
  let http: HttpTestingController;

  const sarah: Client = { id: 1, name: 'Sarah Chen', subaccountNumber: 'SA-100001' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClientService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getClients() calls GET /api/clients', () => {
    let result: Client[] = [];
    service.getClients().subscribe((clients) => (result = clients));

    const req = http.expectOne('/api/clients');
    expect(req.request.method).toBe('GET');
    req.flush([sarah]);

    expect(result).toEqual([sarah]);
  });

  it('getClient(id) calls GET /api/clients/{id}', () => {
    let result: Client | undefined;
    service.getClient(1).subscribe((client) => (result = client));

    http.expectOne('/api/clients/1').flush(sarah);

    expect(result).toEqual(sarah);
  });

  it('getClient(id) reports a 404 as an error (the page shows "not found")', () => {
    let status = 0;
    service.getClient(999).subscribe({ error: (err) => (status = err.status) });

    http.expectOne('/api/clients/999').flush(null, { status: 404, statusText: 'Not Found' });

    expect(status).toBe(404);
  });

  it('createClient() POSTs the request and returns the saved client with its new id', () => {
    const request = { name: 'Ali Khan', subaccountNumber: 'SA-100004' };
    let created: Client | undefined;

    service.createClient(request).subscribe((client) => (created = client));

    const req = http.expectOne('/api/clients');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ id: 4, ...request }, { status: 201, statusText: 'Created' });

    expect(created?.id).toBe(4);
  });

  it('createClient() reports a 409 (duplicate subaccount) as an error', () => {
    let status = 0;
    service
      .createClient({ name: 'Copy', subaccountNumber: 'SA-100001' })
      .subscribe({ error: (err) => (status = err.status) });

    http.expectOne('/api/clients').flush(null, { status: 409, statusText: 'Conflict' });

    expect(status).toBe(409);
  });

  it('getHoldings(id) calls GET /api/clients/{id}/holdings', () => {
    const holding: Holding = {
      fundId: 1,
      ticker: 'MPL100',
      fundName: 'Maple Balanced Growth Fund',
      units: 40,
      nav: 12.5,
      navDate: '2026-11-10',
      marketValue: 500,
    };
    let result: Holding[] = [];

    service.getHoldings(1).subscribe((holdings) => (result = holdings));

    http.expectOne('/api/clients/1/holdings').flush([holding]);

    expect(result).toEqual([holding]);
  });
});
