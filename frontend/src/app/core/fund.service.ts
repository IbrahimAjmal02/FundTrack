import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { CreateNavPriceRequest, Fund, NavPrice } from './models';

/** Talks to the backend's FundController. All URLs go through the dev proxy (/api → :8080). */
@Service()
export class FundService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/funds';

  /** GET /funds: every fund (for the Place order dropdown). */
  getFunds(): Observable<Fund[]> {
    return this.http.get<Fund[]>(this.baseUrl);
  }

  /** GET /funds/{id}/nav?date=YYYY-MM-DD: that fund's NAV for one date (a list of 0 or 1). */
  getNavs(fundId: number, date: string): Observable<NavPrice[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<NavPrice[]>(`${this.baseUrl}/${fundId}/nav`, { params });
  }

  /** POST /funds/{id}/nav: record a fund's NAV for a date (201, or 409 if that date already has one). */
  addNav(fundId: number, request: CreateNavPriceRequest): Observable<NavPrice> {
    return this.http.post<NavPrice>(`${this.baseUrl}/${fundId}/nav`, request);
  }
}
