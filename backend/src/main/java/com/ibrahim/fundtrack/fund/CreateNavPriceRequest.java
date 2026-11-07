package com.ibrahim.fundtrack.fund;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.*;

// Body of POST /funds/{id}/nav, e.g. { "priceDate": "2026-11-03", "nav": 12.5100 }
// The fund comes from the URL ({id}), so it is not a field here.
public record CreateNavPriceRequest(
                @NotNull @PastOrPresent LocalDate priceDate,
                @NotNull @Positive @Digits(integer = 8, fraction = 4) BigDecimal nav) {
}
