package com.ibrahim.fundtrack.order;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

// Body of POST /orders
//   BUY:  { "clientId": 1, "fundId": 1, "orderType": "BUY",  "amount": 500.00 }
//   SELL: { "clientId": 2, "fundId": 2, "orderType": "SELL", "units": 20.0000 }
//
// No orderDate field: the server stamps every order with TODAY's date.
// Letting the caller pick a past date would allow "late trading" (placing an order
// after the NAV is already known), which is illegal in the mutual fund industry.
public record PlaceOrderRequest(
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long fundId,
        @NotNull OrderType orderType,

        // Optional on their own; which one is required depends on orderType (see below).
        // @Positive and @Digits ignore null, so they only apply when the field is sent.
        @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,  // NUMERIC(14, 2)
        @Positive @Digits(integer = 12, fraction = 4) BigDecimal units) { // NUMERIC(16, 4)

    // Cross-field rule: a single-field annotation can't say "required IF orderType is BUY".
    // @AssertTrue runs this method during @Valid; returning false → 400 Bad Request.
    @AssertTrue(message = "BUY orders need an amount (and no units); SELL orders need units (and no amount)")
    public boolean isAmountOrUnitsMatchingOrderType() {
        if (orderType == null) {
            return true; // @NotNull on orderType already reports this; don't report it twice
        }
        return switch (orderType) {
            case BUY -> amount != null && units == null;
            case SELL -> units != null && amount == null;
        };
    }
}
