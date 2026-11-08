package com.ibrahim.fundtrack.order;

// Where an order is in the forward-pricing workflow.
// Matches the DB rule: CHECK (status IN ('PENDING', 'SETTLED', 'REJECTED'))
//   PENDING  = placed during the day, waiting for that day's NAV + "Process day"
//   SETTLED  = priced at the day's NAV; units/amount are final
//   REJECTED = invalid at processing time (e.g. selling more units than the client owns)
public enum OrderStatus {
    PENDING,
    SETTLED,
    REJECTED
}
