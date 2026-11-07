package com.ibrahim.fundtrack.fund;

import java.math.BigDecimal;
import java.time.LocalDate;

// What the API returns for a NAV: flat and simple, no nested Fund entity.
// e.g. { "fundId": 2, "ticker": "NRT200", "priceDate": "2026-11-03", "nav": 24.9800 }
public record NavPriceResponse(Long fundId, String ticker, LocalDate priceDate, BigDecimal nav) {

    // Converts an entity into the response shape (entity → DTO)
    public static NavPriceResponse from(NavPrice navPrice) {
        return new NavPriceResponse(
                navPrice.getFund().getId(),
                navPrice.getFund().getTicker(),
                navPrice.getPriceDate(),
                navPrice.getNav());
    }
}
