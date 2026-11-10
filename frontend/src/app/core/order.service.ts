import { Service, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Order, OrderStatus } from './models';

/** Talks to the backend's OrderController. All URLs go through the dev proxy (/api → :8080). */
@Service()
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/orders';

  /**
   * GET /orders with optional filters:
   *   getOrders()                          → /orders
   *   getOrders({ clientId: 3 })           → /orders?clientId=3
   *   getOrders({ status: 'PENDING' })     → /orders?status=PENDING
   */
  getOrders(filters: { clientId?: number; status?: OrderStatus } = {}): Observable<Order[]> {
    let params = new HttpParams();
    if (filters.clientId != null) {
      params = params.set('clientId', filters.clientId);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<Order[]>(this.baseUrl, { params });
  }
}
