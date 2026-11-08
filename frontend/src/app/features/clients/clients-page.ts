import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Client } from '../../core/models';
import { SAMPLE_CLIENTS } from '../../core/sample-data';

@Component({
  selector: 'app-clients-page',
  imports: [RouterLink],
  templateUrl: './clients-page.html',
  styleUrl: './clients-page.css',
})
export class ClientsPage {
  // TODO (you): replace the sample data with a ClientService call
  //   1. create core/client.service.ts with HttpClient: getClients() → GET /api/clients
  //   2. inject it here and load the list into this signal
  protected readonly clients = signal<Client[]>(SAMPLE_CLIENTS);

  // TODO (you): Reactive Form (or Signal Form) for "Add client" with validation that mirrors
  //   the backend: name required (max 100), subaccount required + pattern SA-######.
  //   On submit → ClientService.createClient() → POST /api/clients → add the new client to the list.
  //   Handle 409 (subaccount already exists) by showing an error under the field.
}
