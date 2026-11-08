package com.ibrahim.fundtrack.holding;

import java.math.BigDecimal;
import java.time.LocalDate;

// One line of GET /clients/{id}/holdings: what the client owns in one fund, and what it's worth.
// nav / navDate / marketValue are null only if the fund has never had a NAV entered.
public record HoldingResponse(
        Long fundId,
        String ticker,
        String fundName,
        BigDecimal units,        // settled BUY units − settled SELL units (4 decimals)
        BigDecimal nav,          // the fund's LATEST NAV
        LocalDate navDate,       // the date of that NAV
        BigDecimal marketValue) { // units × nav, rounded to the cent
}
