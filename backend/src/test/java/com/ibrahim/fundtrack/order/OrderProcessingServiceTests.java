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
        // Given: Sarah has a PENDING order to buy $500 of Maple, and Maple's NAV that
        // day is $12.50
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
    @Test
    void shouldRoundBuyUnitsToFourDecimalsHalfUp() {
        // Given: Sarah has a PENDING order to buy $500 of Maple, and Maple's NAV that
        // day is $12.50
        FundOrder buy = FundOrder.buy(sarah, maple, new BigDecimal("200.00"), DAY);
        when(orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, DAY))
                .thenReturn(List.of(buy));
        when(navPriceRepository.findByFundAndPriceDate(maple, DAY))
                .thenReturn(List.of(new NavPrice(maple, DAY, new BigDecimal("3.0000"))));

        // When: the day is processed
        ProcessingResult result = service.processDay(DAY);

        // Then: the order is SETTLED at $12.50, for 500 ÷ 12.50 = 40 units
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.SETTLED);
        assertThat(buy.getPriceUsed()).isEqualByComparingTo("3.0000");
        assertThat(buy.getUnitsSettled()).isEqualByComparingTo("66.6667");
        assertThat(buy.getAmount()).isEqualByComparingTo("200.00");
        assertThat(result.settled()).isEqualTo(1);
    }

    // ── Test 3: SELL the client can cover ────────────────────────────────────
    @Test
    void shouldSettleSellOrderWhenClientHoldsEnoughUnits() {
        // Given: Kumar owns 50 units of Northern, has a PENDING order to SELL 20,
        // and Northern's NAV that day is $25.10
        Client kumar = new Client("Kumar Patel", "SA-100002");
        Fund northern = new Fund("NRT200", "Northern Canadian Equity Fund");
        FundOrder sell = FundOrder.sell(kumar, northern, new BigDecimal("20.0000"), DAY);

        when(orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, DAY))
                .thenReturn(List.of(sell));
        when(navPriceRepository.findByFundAndPriceDate(northern, DAY))
                .thenReturn(List.of(new NavPrice(northern, DAY, new BigDecimal("25.1000"))));
        when(orderRepository.unitsHeld(kumar, northern)) // "how many units does Kumar own?" → 50
                .thenReturn(new BigDecimal("50.0000"));

        // When: the day is processed
        ProcessingResult result = service.processDay(DAY);

        // Then: SETTLED at $25.10; 20 units sold; he receives 20 × 25.10 = $502.00
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.SETTLED);
        assertThat(sell.getPriceUsed()).isEqualByComparingTo("25.1000");
        assertThat(sell.getUnitsSettled()).isEqualByComparingTo("20.0000");
        assertThat(sell.getAmount()).isEqualByComparingTo("502.00");
        assertThat(sell.getAmount().scale()).isEqualTo(2); // dollars are stored to the cent
        assertThat(result.settled()).isEqualTo(1);
    }

    // ── Test 4: SELL more units than the client holds ───────────────────────
    @Test
    void shouldRejectSellOrderWhenClientHoldsTooFewUnits() {
        // Given: Kumar owns only 15 units of Northern, but has a PENDING order to SELL 20
        Client kumar = new Client("Kumar Patel", "SA-100002");
        Fund northern = new Fund("NRT200", "Northern Canadian Equity Fund");
        FundOrder sell = FundOrder.sell(kumar, northern, new BigDecimal("20.0000"), DAY);

        when(orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, DAY))
                .thenReturn(List.of(sell));
        when(navPriceRepository.findByFundAndPriceDate(northern, DAY))
                .thenReturn(List.of(new NavPrice(northern, DAY, new BigDecimal("25.1000"))));
        when(orderRepository.unitsHeld(kumar, northern))
                .thenReturn(new BigDecimal("15.0000"));

        // When
        ProcessingResult result = service.processDay(DAY);

        // Then: REJECTED; nothing was priced; the summary counts 1 rejected, 0 settled
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(sell.getPriceUsed()).isNull(); // a rejected order is never priced
        assertThat(result.rejected()).isEqualTo(1);
        assertThat(result.settled()).isEqualTo(0);
    }

    // ── Test 4b: boundary, selling EXACTLY what you hold is allowed ─────────
    @Test
    void shouldSettleSellOrderForExactlyAllUnitsHeld() {
        // Given: Kumar owns exactly 20 units and sells all 20
        Client kumar = new Client("Kumar Patel", "SA-100002");
        Fund northern = new Fund("NRT200", "Northern Canadian Equity Fund");
        FundOrder sell = FundOrder.sell(kumar, northern, new BigDecimal("20.0000"), DAY);

        when(orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, DAY))
                .thenReturn(List.of(sell));
        when(navPriceRepository.findByFundAndPriceDate(northern, DAY))
                .thenReturn(List.of(new NavPrice(northern, DAY, new BigDecimal("25.1000"))));
        when(orderRepository.unitsHeld(kumar, northern))
                .thenReturn(new BigDecimal("20.0000"));

        // When
        service.processDay(DAY);

        // Then: SETTLED (20 ≤ 20). Catches a "<" written where "<=" was meant.
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.SETTLED);
    }

    // ── Test 5: no NAV for the date → the order WAITS ───────────────────────
    @Test
    void shouldLeaveOrderPendingWhenFundHasNoNavForTheDate() {
        // Given: Sarah's BUY is pending, but nobody has entered Maple's NAV for that day
        FundOrder buy = FundOrder.buy(sarah, maple, new BigDecimal("500.00"), DAY);

        when(orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, DAY))
                .thenReturn(List.of(buy));
        when(navPriceRepository.findByFundAndPriceDate(maple, DAY))
                .thenReturn(List.of()); // empty list = no NAV

        // When
        ProcessingResult result = service.processDay(DAY);

        // Then: still PENDING, never priced, counted as still pending (nothing crashed)
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(buy.getPriceUsed()).isNull();
        assertThat(result.stillPending()).isEqualTo(1);
        assertThat(result.settled()).isEqualTo(0);
    }

    // ── Test 5b: a missing NAV for ONE fund doesn't block the others ────────
    @Test
    void shouldStillSettleOtherFundsWhenOneFundHasNoNav() {
        // Given: Maple HAS a NAV today, Northern does NOT. One BUY in each.
        Fund northern = new Fund("NRT200", "Northern Canadian Equity Fund");
        FundOrder mapleBuy = FundOrder.buy(sarah, maple, new BigDecimal("500.00"), DAY);
        FundOrder northernBuy = FundOrder.buy(sarah, northern, new BigDecimal("300.00"), DAY);

        when(orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, DAY))
                .thenReturn(List.of(northernBuy, mapleBuy)); // the one without a NAV comes FIRST
        when(navPriceRepository.findByFundAndPriceDate(northern, DAY))
                .thenReturn(List.of());
        when(navPriceRepository.findByFundAndPriceDate(maple, DAY))
                .thenReturn(List.of(new NavPrice(maple, DAY, new BigDecimal("12.5000"))));

        // When
        ProcessingResult result = service.processDay(DAY);

        // Then: Northern waits, Maple settles. Processing CONTINUED past the missing NAV.
        assertThat(northernBuy.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(mapleBuy.getStatus()).isEqualTo(OrderStatus.SETTLED);
        assertThat(result.settled()).isEqualTo(1);
        assertThat(result.stillPending()).isEqualTo(1);
    }

    // ── Test 6: two SELLs on the same day that TOGETHER exceed holdings ─────
    @Test
    void shouldRejectSecondSellWhenSameDaySellsTogetherExceedHoldings() {
        // Given: Kumar owns 15 units. Two PENDING SELLs of 10 each, same day, same fund.
        //   Each looks fine on its own (10 ≤ 15), but together they're 20 > 15.
        Client kumar = new Client("Kumar Patel", "SA-100002");
        Fund northern = new Fund("NRT200", "Northern Canadian Equity Fund");
        FundOrder firstSell = FundOrder.sell(kumar, northern, new BigDecimal("10.0000"), DAY);
        FundOrder secondSell = FundOrder.sell(kumar, northern, new BigDecimal("10.0000"), DAY);

        when(orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, DAY))
                .thenReturn(List.of(firstSell, secondSell)); // oldest first
        when(navPriceRepository.findByFundAndPriceDate(northern, DAY))
                .thenReturn(List.of(new NavPrice(northern, DAY, new BigDecimal("25.1000"))));
        // Holdings BEFORE today's processing. (A mock always gives the same answer:
        // it does NOT know the first SELL already used up 10 of these units.)
        when(orderRepository.unitsHeld(kumar, northern))
                .thenReturn(new BigDecimal("15.0000"));

        // When
        ProcessingResult result = service.processDay(DAY);

        // Then: first-come, first-served. 15 − 10 = 5 left, so the second SELL of 10 is rejected.
        assertThat(firstSell.getStatus()).isEqualTo(OrderStatus.SETTLED);
        assertThat(secondSell.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(result.settled()).isEqualTo(1);
        assertThat(result.rejected()).isEqualTo(1);
    }
}
