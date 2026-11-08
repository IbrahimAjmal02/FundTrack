package com.ibrahim.fundtrack.holding;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibrahim.fundtrack.client.Client;
import com.ibrahim.fundtrack.fund.NavPrice;
import com.ibrahim.fundtrack.fund.NavPriceRepository;
import com.ibrahim.fundtrack.order.FundOrderRepository;

// Holdings are CALCULATED, never stored: units come from settled orders,
// value comes from each fund's latest NAV. One source of truth: the order history.
@Service
public class HoldingService {
    private final FundOrderRepository orderRepository;
    private final NavPriceRepository navPriceRepository;

    public HoldingService(FundOrderRepository orderRepository, NavPriceRepository navPriceRepository) {
        this.orderRepository = orderRepository;
        this.navPriceRepository = navPriceRepository;
    }

    // readOnly = true: one transaction for both queries, and tells Hibernate nothing will change
    @Transactional(readOnly = true)
    public List<HoldingResponse> holdingsFor(Client client) {
        List<HoldingResponse> holdings = new ArrayList<>();

        // 1. How many units of each fund does the client hold? (one query, GROUP BY fund)
        for (UnitsByFund row : orderRepository.unitsByFund(client)) {

            // 2. The fund's most recent NAV (may not exist if no NAV was ever entered)
            Optional<NavPrice> latest = navPriceRepository.findFirstByFundIdOrderByPriceDateDesc(row.fundId());

            if (latest.isEmpty()) {
                // Still show what they own; we just can't put a dollar value on it yet
                holdings.add(new HoldingResponse(row.fundId(), row.ticker(), row.fundName(),
                        row.units(), null, null, null));
                continue;
            }

            // 3. Market value = units × latest NAV, rounded to the cent
            BigDecimal nav = latest.get().getNav();
            BigDecimal marketValue = row.units().multiply(nav).setScale(2, RoundingMode.HALF_UP);

            holdings.add(new HoldingResponse(row.fundId(), row.ticker(), row.fundName(),
                    row.units(), nav, latest.get().getPriceDate(), marketValue));
        }

        return holdings;
    }
}
