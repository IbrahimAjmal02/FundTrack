package com.ibrahim.fundtrack.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ibrahim.fundtrack.client.Client;
import com.ibrahim.fundtrack.fund.Fund;
import com.ibrahim.fundtrack.fund.NavPrice;
import com.ibrahim.fundtrack.fund.NavPriceRepository;

// UNIT tests: no Spring, no database, no HTTP. Runs in milliseconds.
// The repositories are Mockito "mocks" (fakes): each test tells them exactly what to return.
@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTests {

    @Mock
    FundOrderRepository orderRepository;

    @Mock
    NavPriceRepository navPriceRepository;

    @InjectMocks
    OrderProcessingService service; // a REAL service, built with the two mocks above

    // Shared test data: plain Java objects, nothing saved anywhere
    static final LocalDate DAY = LocalDate.of(2026, 11, 3);
    final Client sarah = new Client("Sarah Chen", "SA-100001");
    final Fund maple = new Fund("MPL100", "Maple Balanced Growth Fund");

    // ── Test 1 (worked example) ──────────────────────────────────────────────
    @Test
    void shouldSettleBuyOrderAtTheDaysNav() {
        // Given: Sarah has a PENDING order to buy $500 of Maple, and Maple's NAV that day is $12.50
        FundOrder buy = FundOrder.buy(sarah, maple, new BigDecimal("500.00"), DAY);
        when(orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, DAY))
                .thenReturn(List.of(buy));
        when(navPriceRepository.findByFundAndPriceDate(maple, DAY))
                .thenReturn(List.of(new NavPrice(maple, DAY, new BigDecimal("12.5000"))));

        // When: the day is processed
        ProcessingResult result = service.processDay(DAY);

        // Then: the order is SETTLED at $12.50, for 500 ÷ 12.50 = 40 units
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.SETTLED);
        assertThat(buy.getPriceUsed()).isEqualByComparingTo("12.5000");
        assertThat(buy.getUnitsSettled()).isEqualByComparingTo("40.0000");
        assertThat(buy.getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.settled()).isEqualTo(1);
    }

    // ── Your tests go below, one at a time ───────────────────────────────────
    // 2. Rounding: $500 at NAV 12.45 → 40.1606 units (scale 4, HALF_UP)
    // 3. SELL the client can cover → SETTLED, amount = units × NAV (scale 2)
    // 4. SELL more units than held → REJECTED
    // 5. No NAV for the date → stays PENDING
    // 6. Two SELLs that together exceed holdings → first SETTLED, second REJECTED
}
