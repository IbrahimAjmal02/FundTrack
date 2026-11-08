package com.ibrahim.fundtrack.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        int rejected = 0;
        int stillPending = 0;

        // Running tally of units each client can still sell, per (client, fund), during THIS run.
        // Key example: "SA-100002|NRT200" → 5.0000
        // Filled the first time a client+fund is needed; then updated after every settle,
        // so two same-day SELLs can't spend the same units twice.
        Map<String, BigDecimal> availableUnits = new HashMap<>();

        for (FundOrder order : orders) {
            // The day's NAV for this order's fund (same lookup for BUY and SELL)
            List<NavPrice> navs = navPriceRepository.findByFundAndPriceDate(order.getFund(), date);

            if (navs.isEmpty()) {
                stillPending++;
                continue;
            }

            BigDecimal nav = navs.get(0).getNav();
            String key = order.getClient().getSubaccountNumber() + "|" + order.getFund().getTicker();

            if (order.getOrderType() == OrderType.BUY) {
                // BUY: dollars in → units out. units = amount ÷ NAV (4 decimals)
                BigDecimal units = order.getAmount().divide(nav, 4, RoundingMode.HALF_UP);
                order.settle(nav, units, order.getAmount());
                settled++;

                // If we're already tracking this client+fund, the new units can back a later SELL today
                if (availableUnits.containsKey(key)) {
                    availableUnits.put(key, availableUnits.get(key).add(units));
                }
            } else {
                // SELL: units in → dollars out. How many units can this client still sell?
                // First time we see this client+fund: ask the database (holdings before this SELL).
                // After that: use the running tally, which already subtracts today's earlier SELLs.
                BigDecimal available = availableUnits.get(key);
                if (available == null) {
                    available = orderRepository.unitsHeld(order.getClient(), order.getFund());
                }

                if (order.getUnits().compareTo(available) <= 0) { // units to sell ≤ units available
                    // amount = units × NAV, rounded to the cent (2 decimals)
                    BigDecimal amount = order.getUnits().multiply(nav).setScale(2, RoundingMode.HALF_UP);
                    order.settle(nav, order.getUnits(), amount);
                    settled++;
                    availableUnits.put(key, available.subtract(order.getUnits())); // spend them
                } else {
                    availableUnits.put(key, available); // remember the (unchanged) balance
                    order.reject();
                    rejected++;
                }
            }
        }

        return new ProcessingResult(date, settled, rejected, stillPending);
    }
}
