import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Client, CreateClientRequest, Holding } from './models';

/** Talks to the backend's ClientController. All URLs go through the dev proxy (/api → :8080). */
@Service()
export class ClientService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/clients';

  /** GET /clients: every client. */
  getClients(): Observable<Client[]> {
    return this.http.get<Client[]>(this.baseUrl);
  }

  /** GET /clients/{id}: one client (404 if missing). */
  getClient(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.baseUrl}/${id}`);
  }

  /** POST /clients: create a client (201, or 409 if the subaccount number is taken). */
  createClient(request: CreateClientRequest): Observable<Client> {
    return this.http.post<Client>(this.baseUrl, request);
  }

  /** GET /clients/{id}/holdings: units and market value per fund. */
  getHoldings(id: number): Observable<Holding[]> {
    return this.http.get<Holding[]>(`${this.baseUrl}/${id}/holdings`);
  }
}
