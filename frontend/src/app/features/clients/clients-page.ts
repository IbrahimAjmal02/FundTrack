import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ClientService } from '../../core/client.service';
import { Client } from '../../core/models';

@Component({
  selector: 'app-clients-page',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './clients-page.html',
  styleUrl: './clients-page.css',
})
export class ClientsPage implements OnInit {
  private readonly clientService = inject(ClientService);

  protected readonly clients = signal<Client[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly saving = signal(false); // true while the POST is in flight
  protected readonly saveError = signal<string | null>(null); // e.g. 409 duplicate

  // "Add client" form: the same rules as the backend's CreateClientRequest
  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),
    subaccountNumber: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^SA-\d{6}$/)],
    }),
  });

  ngOnInit(): void {
    this.clientService.getClients().subscribe({
      next: (clients) => {
        this.clients.set(clients);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load clients. Is the backend running on port 8080?');
        this.loading.set(false);
      },
    });
  }

  addClient(): void {
    if (this.saving()) return; // already sending: ignore extra clicks

    if (this.form.invalid) {
      this.form.markAllAsTouched(); // show every field's error message
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);

    const request = this.form.getRawValue(); // { name, subaccountNumber }

    this.clientService.createClient(request).subscribe({
      next: (created) => {
        this.clients.update((list) => [...list, created]);
        this.form.reset();
        this.saving.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.saveError.set(
          err.status === 409
            ? `Subaccount ${request.subaccountNumber} already exists.`
            : 'Could not add the client. Please try again.',
        );
        this.saving.set(false);
      },
    });
  }
}
