package com.ibrahim.fundtrack.holding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ibrahim.fundtrack.client.Client;
import com.ibrahim.fundtrack.fund.Fund;
import com.ibrahim.fundtrack.fund.NavPrice;
import com.ibrahim.fundtrack.fund.NavPriceRepository;
import com.ibrahim.fundtrack.order.FundOrderRepository;

// Unit tests for the market-value maths (Mockito fakes, no database).
// The integration test covers the real queries; these cover the edge cases cheaply.
@ExtendWith(MockitoExtension.class)
class HoldingServiceTests {

    @Mock
    FundOrderRepository orderRepository;

    @Mock
    NavPriceRepository navPriceRepository;

    @InjectMocks
    HoldingService service;

    final Client sarah = new Client("Sarah Chen", "SA-100001");
    final Fund maple = new Fund("MPL100", "Maple Balanced Growth Fund");

    @Test
    void shouldRoundMarketValueToTheCent() {
        // 40.1606 units × 12.4500 = 499.999470 → 500.00
        when(orderRepository.unitsByFund(sarah)).thenReturn(List.of(
                new UnitsByFund(1L, "MPL100", "Maple Balanced Growth Fund", new BigDecimal("40.1606"))));
        when(navPriceRepository.findFirstByFundIdOrderByPriceDateDesc(1L)).thenReturn(Optional.of(
                new NavPrice(maple, LocalDate.of(2026, 11, 2), new BigDecimal("12.4500"))));

        List<HoldingResponse> holdings = service.holdingsFor(sarah);

        assertThat(holdings).hasSize(1);
        assertThat(holdings.get(0).marketValue()).isEqualByComparingTo("500.00");
        assertThat(holdings.get(0).marketValue().scale()).isEqualTo(2);
        assertThat(holdings.get(0).navDate()).isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void shouldStillListHoldingWhenFundHasNeverHadANav() {
        when(orderRepository.unitsByFund(sarah)).thenReturn(List.of(
                new UnitsByFund(1L, "MPL100", "Maple Balanced Growth Fund", new BigDecimal("10.0000"))));
        when(navPriceRepository.findFirstByFundIdOrderByPriceDateDesc(1L)).thenReturn(Optional.empty());

        List<HoldingResponse> holdings = service.holdingsFor(sarah);

        // The units are still shown; only the value is unknown
        assertThat(holdings).hasSize(1);
        assertThat(holdings.get(0).units()).isEqualByComparingTo("10.0000");
        assertThat(holdings.get(0).nav()).isNull();
        assertThat(holdings.get(0).marketValue()).isNull();
    }
}
