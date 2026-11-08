// TypeScript shapes of the JSON your Spring API sends and receives.
// Each one mirrors a Java record/entity: same field names, because Jackson uses them as JSON keys.
//
// Money and units: the API sends numbers like 500.00 and 40.1606. We keep them as `number`
// for DISPLAY only. All money maths happens on the server in BigDecimal; the UI never calculates it.

export type OrderType = 'BUY' | 'SELL';
export type OrderStatus = 'PENDING' | 'SETTLED' | 'REJECTED';

// GET /clients, GET /clients/{id}           ← Java: Client entity
export interface Client {
  id: number;
  name: string;
  subaccountNumber: string;
}

// POST /clients body                         ← Java: CreateClientRequest
export interface CreateClientRequest {
  name: string;
  subaccountNumber: string;
}

// GET /funds                                 ← Java: Fund entity
export interface Fund {
  id: number;
  ticker: string;
  name: string;
}

// GET /funds/{id}/nav, POST /funds/{id}/nav  ← Java: NavPriceResponse
export interface NavPrice {
  fundId: number;
  ticker: string;
  priceDate: string; // ISO date, e.g. "2026-11-08"
  nav: number;
}

// POST /funds/{id}/nav body                  ← Java: CreateNavPriceRequest
export interface CreateNavPriceRequest {
  priceDate: string;
  nav: number;
}

// GET /orders, GET /orders/{id}              ← Java: OrderResponse
export interface Order {
  id: number;
  clientId: number;
  clientName: string;
  fundId: number;
  ticker: string;
  orderType: OrderType;
  amount: number | null; // BUY: entered. SELL: filled in at settlement
  units: number | null; // SELL: entered. BUY: null
  orderDate: string;
  status: OrderStatus;
  priceUsed: number | null; // null until settled
  unitsSettled: number | null; // null until settled
  createdAt: string; // ISO date-time
}

// POST /orders body                          ← Java: PlaceOrderRequest
export interface PlaceOrderRequest {
  clientId: number;
  fundId: number;
  orderType: OrderType;
  amount?: number; // BUY only
  units?: number; // SELL only
}

// GET /clients/{id}/holdings                 ← Java: HoldingResponse
export interface Holding {
  fundId: number;
  ticker: string;
  fundName: string;
  units: number;
  nav: number | null;
  navDate: string | null;
  marketValue: number | null;
}

// POST /orders/process?date=...              ← Java: ProcessingResult
export interface ProcessingResult {
  date: string;
  settled: number;
  rejected: number;
  stillPending: number;
}
