package com.ibrahim.fundtrack.order;

// What kind of order it is. Matches the DB rule: CHECK (order_type IN ('BUY', 'SELL'))
//   BUY  = purchase, placed by dollar AMOUNT   (units worked out at processing: amount ÷ NAV)
//   SELL = redemption, placed by number of UNITS (amount worked out at processing: units × NAV)
public enum OrderType {
    BUY,
    SELL
}
