package com.ibrahim.fundtrack.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// What the API returns for an order: flat, with the client's name and fund's ticker pulled in.
// priceUsed / unitsSettled stay null until "Process day" settles the order.
public record OrderResponse(
        Long id,
        Long clientId,
        String clientName,
        Long fundId,
        String ticker,
        OrderType orderType,
        BigDecimal amount,
        BigDecimal units,
        LocalDate orderDate,
        OrderStatus status,
        BigDecimal priceUsed,
        BigDecimal unitsSettled,
        LocalDateTime createdAt) {

    // Entity → DTO. Reads order.getClient() and order.getFund(), so the order must have been
    // loaded with them (the @EntityGraph queries), or created in this request with real objects.
    public static OrderResponse from(FundOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getName(),
                order.getFund().getId(),
                order.getFund().getTicker(),
                order.getOrderType(),
                order.getAmount(),
                order.getUnits(),
                order.getOrderDate(),
                order.getStatus(),
                order.getPriceUsed(),
                order.getUnitsSettled(),
                order.getCreatedAt());
    }
}
