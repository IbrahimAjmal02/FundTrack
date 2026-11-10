import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { FundService } from './fund.service';
import { Fund, NavPrice } from './models';

describe('FundService', () => {
  let service: FundService;
  let http: HttpTestingController;

  const mpl: Fund = { id: 1, ticker: 'MPL100', name: 'Maple Balanced Growth Fund' };
  const mplNav: NavPrice = { fundId: 1, ticker: 'MPL100', priceDate: '2026-11-10', nav: 12.5 };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FundService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getFunds() calls GET /api/funds', () => {
    let result: Fund[] = [];
    service.getFunds().subscribe((funds) => (result = funds));

    const req = http.expectOne('/api/funds');
    expect(req.request.method).toBe('GET');
    req.flush([mpl]);

    expect(result).toEqual([mpl]);
  });

  it('getNavs(id, date) calls GET /api/funds/{id}/nav?date=', () => {
    let result: NavPrice[] = [];
    service.getNavs(1, '2026-11-10').subscribe((navs) => (result = navs));

    const req = http.expectOne('/api/funds/1/nav?date=2026-11-10');
    expect(req.request.method).toBe('GET');
    req.flush([mplNav]);

    expect(result).toEqual([mplNav]);
  });

  it('addNav() POSTs the date and NAV to /api/funds/{id}/nav', () => {
    let saved: NavPrice | undefined;
    const request = { priceDate: '2026-11-10', nav: 12.5 };

    service.addNav(1, request).subscribe((nav) => (saved = nav));

    const req = http.expectOne('/api/funds/1/nav');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mplNav, { status: 201, statusText: 'Created' });

    expect(saved).toEqual(mplNav);
  });

  it('addNav() reports a 409 (NAV already entered for that date) as an error', () => {
    let status = 0;
    service
      .addNav(1, { priceDate: '2026-11-10', nav: 13 })
      .subscribe({ error: (err) => (status = err.status) });

    http.expectOne('/api/funds/1/nav').flush(null, { status: 409, statusText: 'Conflict' });

    expect(status).toBe(409);
  });
});
