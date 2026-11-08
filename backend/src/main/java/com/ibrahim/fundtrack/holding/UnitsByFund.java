package com.ibrahim.fundtrack.holding;

import java.math.BigDecimal;

// One row of the "units per fund" query: which fund, and how many units the client holds in it.
// Built directly by JPQL with a constructor expression: SELECT new ...UnitsByFund(...)
public record UnitsByFund(Long fundId, String ticker, String fundName, BigDecimal units) {
}
