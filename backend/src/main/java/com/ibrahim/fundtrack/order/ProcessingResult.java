package com.ibrahim.fundtrack.order;

import java.time.LocalDate;

// Summary returned by "Process day", e.g. { "date": "2026-11-03", "settled": 5, "rejected": 1, "stillPending": 2 }
public record ProcessingResult(LocalDate date, int settled, int rejected, int stillPending) {
}
