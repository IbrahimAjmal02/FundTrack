package com.ibrahim.fundtrack.fund;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "nav_price")
public class NavPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many NAV rows belong to one fund. The fund_id column holds the foreign key;
    // Hibernate loads the Fund only when getFund() is first used (LAZY).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fund_id")
    private Fund fund;

    // SQL DATE → LocalDate (no time, no timezone). Maps to price_date.
    @Column(nullable = false)
    private LocalDate priceDate;

    // NUMERIC(12, 4) → BigDecimal: exact decimal, 4 places (e.g. 12.4500)
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal nav;

    protected NavPrice() {
    }

    public NavPrice(Fund fund, LocalDate priceDate, BigDecimal nav) {
        this.fund = fund;
        this.priceDate = priceDate;
        this.nav = nav;
    }

    public Long getId() {
        return this.id;
    }

    public Fund getFund() {
        return this.fund;
    }

    public LocalDate getPriceDate() {
        return this.priceDate;
    }

    public BigDecimal getNav() {
        return this.nav;
    }
}
