package com.ibrahim.fundtrack.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.ibrahim.fundtrack.client.Client;
import com.ibrahim.fundtrack.fund.Fund;

import jakarta.persistence.*;

@Entity
@Table(name = "fund_order")
public class FundOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── WHO and WHAT: two relationships (same pattern as NavPrice → Fund, twice) ──
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fund_id")
    private Fund fund;

    // ── WHAT KIND: enums stored as their NAME ("BUY", "PENDING"), never as 0/1/2 ──
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private OrderType orderType;

    // ── HOW MUCH: a BUY has an amount, a SELL has units. The other one starts null ──
    @Column(precision = 14, scale = 2)
    private BigDecimal amount;      // dollars, 2 decimal places   (BUY: input. SELL: filled at settlement)

    @Column(precision = 16, scale = 4)
    private BigDecimal units;       // fund units, 4 decimal places (SELL: input)

    @Column(nullable = false)
    private LocalDate orderDate;    // the trading day → which day's NAV prices it

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderStatus status;

    // ── FILLED IN BY "PROCESS DAY" (null while PENDING) ──
    @Column(precision = 12, scale = 4)
    private BigDecimal priceUsed;    // the NAV the order settled at

    @Column(precision = 16, scale = 4)
    private BigDecimal unitsSettled; // units actually bought or sold

    // ── SET AUTOMATICALLY: Hibernate fills this with "now" on insert; never changes ──
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FundOrder() {
    }

    // Private: outside code must use buy(...) or sell(...), so an order can't be
    // created with the wrong field filled in.
    private FundOrder(Client client, Fund fund, OrderType orderType,
            BigDecimal amount, BigDecimal units, LocalDate orderDate) {
        this.client = client;
        this.fund = fund;
        this.orderType = orderType;
        this.amount = amount;
        this.units = units;
        this.orderDate = orderDate;
        this.status = OrderStatus.PENDING; // every order starts PENDING
    }

    // "Sarah buys $500 of MPL100 today"
    public static FundOrder buy(Client client, Fund fund, BigDecimal amount, LocalDate orderDate) {
        return new FundOrder(client, fund, OrderType.BUY, amount, null, orderDate);
    }

    // "Kumar sells 20 units of NRT200 today"
    public static FundOrder sell(Client client, Fund fund, BigDecimal units, LocalDate orderDate) {
        return new FundOrder(client, fund, OrderType.SELL, null, units, orderDate);
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public Fund getFund() {
        return fund;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getPriceUsed() {
        return priceUsed;
    }

    public BigDecimal getUnitsSettled() {
        return unitsSettled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
