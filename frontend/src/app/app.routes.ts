import { Routes } from '@angular/router';

// Each page is lazy-loaded: its code is only downloaded when you first visit it.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'clients' },
  {
    path: 'clients',
    title: 'Clients · FundTrack',
    loadComponent: () => import('./features/clients/clients-page').then((m) => m.ClientsPage),
  },
  {
    path: 'clients/:id',
    title: 'Client · FundTrack',
    loadComponent: () =>
      import('./features/client-detail/client-detail-page').then((m) => m.ClientDetailPage),
  },
  {
    path: 'orders/new',
    title: 'Place order · FundTrack',
    loadComponent: () =>
      import('./features/place-order/place-order-page').then((m) => m.PlaceOrderPage),
  },
  {
    path: 'operations',
    title: 'Operations · FundTrack',
    loadComponent: () =>
      import('./features/operations/operations-page').then((m) => m.OperationsPage),
  },
  { path: '**', redirectTo: 'clients' },
];
