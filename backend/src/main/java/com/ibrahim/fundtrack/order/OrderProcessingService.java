package com.ibrahim.fundtrack.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibrahim.fundtrack.fund.NavPrice;
import com.ibrahim.fundtrack.fund.NavPriceRepository;

// The heart of FundTrack: forward pricing.
// "Process day" prices every PENDING order for a date at that date's NAV.
@Service
public class OrderProcessingService {
    private final FundOrderRepository orderRepository;
    private final NavPriceRepository navPriceRepository;

    public OrderProcessingService(FundOrderRepository orderRepository, NavPriceRepository navPriceRepository) {
        this.orderRepository = orderRepository;
        this.navPriceRepository = navPriceRepository;
    }

    // @Transactional: the whole day is ONE database transaction (all-or-nothing).
    // If anything throws halfway through, every change is rolled back, so no day is
    // half-processed.
    @Transactional
    public ProcessingResult processDay(LocalDate date) {
        List<FundOrder> orders = orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING, date);
        int settled = 0;

        for (FundOrder order : orders) {
            // 1. get the NAV for this order's fund on this date
            // hint: navPriceRepository.findByFundAndPriceDate(order.getFund(), date) → a
            // list; take .get(0).getNav()
            BigDecimal curr = navPriceRepository.findByFundAndPriceDate(order.getFund(), date).get(0).getNav();

            // 2. units = amount ÷ nav, to 4 decimals, HALF_UP
            // hint: order.getAmount().divide(nav, 4, RoundingMode.HALF_UP)
            BigDecimal units = order.getAmount().divide(curr, 4, RoundingMode.HALF_UP);

            // 3. order.settle(nav, units, order.getAmount());
            order.settle(curr, units, order.getAmount());

            settled++;
        }

        return new ProcessingResult(date, settled, 0, 0);
        // TODO (you): implement, one test at a time.
        //
        // Tools you have:
        // orderRepository.findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus.PENDING,
        // date)
        // navPriceRepository.findByFundAndPriceDate(fund, date) → List<NavPrice> (0 or
        // 1)
        // orderRepository.unitsHeld(client, fund) → BigDecimal (for SELLs, later)
        // order.settle(priceUsed, unitsSettled, amount) order.reject()
        // order.getOrderType(), getAmount(), getUnits(), getFund(), getClient()
        //
        // Money rules (CLAUDE.md): units scale 4, amounts scale 2, RoundingMode.HALF_UP
    }
}
