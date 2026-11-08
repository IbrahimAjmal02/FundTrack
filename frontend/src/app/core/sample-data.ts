// ⚠️ TEMPORARY SAMPLE DATA so the pages render before the API is wired in.
// Shapes match core/models.ts exactly, so swapping in real service calls is a drop-in change.
// Once every page uses real services, delete this file (the build will tell you if anything still uses it).

import { Client, Fund, Holding, Order } from './models';

export const SAMPLE_CLIENTS: Client[] = [
  { id: 1, name: 'Sarah Chen', subaccountNumber: 'SA-100001' },
  { id: 2, name: 'Kumar Patel', subaccountNumber: 'SA-100002' },
  { id: 3, name: 'Maria Lopez', subaccountNumber: 'SA-100003' },
];

export const SAMPLE_FUNDS: Fund[] = [
  { id: 1, ticker: 'MPL100', name: 'Maple Balanced Growth Fund' },
  { id: 2, ticker: 'NRT200', name: 'Northern Canadian Equity Fund' },
  { id: 3, ticker: 'BND300', name: 'Harbour Bond Income Fund' },
];

export const SAMPLE_HOLDINGS: Holding[] = [
  {
    fundId: 1,
    ticker: 'MPL100',
    fundName: 'Maple Balanced Growth Fund',
    units: 50.1606,
    nav: 12.5,
    navDate: '2026-11-08',
    marketValue: 627.01,
  },
  {
    fundId: 2,
    ticker: 'NRT200',
    fundName: 'Northern Canadian Equity Fund',
    units: 15,
    nav: 25.1,
    navDate: '2026-11-07',
    marketValue: 376.5,
  },
];

export const SAMPLE_ORDERS: Order[] = [
  {
    id: 7,
    clientId: 1,
    clientName: 'Sarah Chen',
    fundId: 1,
    ticker: 'MPL100',
    orderType: 'BUY',
    amount: 250,
    units: null,
    orderDate: '2026-11-08',
    status: 'PENDING',
    priceUsed: null,
    unitsSettled: null,
    createdAt: '2026-11-08T10:15:30',
  },
  {
    id: 5,
    clientId: 2,
    clientName: 'Kumar Patel',
    fundId: 2,
    ticker: 'NRT200',
    orderType: 'SELL',
    amount: null,
    units: 20,
    orderDate: '2026-11-08',
    status: 'PENDING',
    priceUsed: null,
    unitsSettled: null,
    createdAt: '2026-11-08T11:02:11',
  },
  {
    id: 3,
    clientId: 1,
    clientName: 'Sarah Chen',
    fundId: 1,
    ticker: 'MPL100',
    orderType: 'BUY',
    amount: 500,
    units: null,
    orderDate: '2026-11-07',
    status: 'SETTLED',
    priceUsed: 12.45,
    unitsSettled: 40.1606,
    createdAt: '2026-11-07T09:41:02',
  },
  {
    id: 2,
    clientId: 1,
    clientName: 'Sarah Chen',
    fundId: 2,
    ticker: 'NRT200',
    orderType: 'SELL',
    amount: null,
    units: 99,
    orderDate: '2026-11-06',
    status: 'REJECTED',
    priceUsed: null,
    unitsSettled: null,
    createdAt: '2026-11-06T14:20:45',
  },
];
