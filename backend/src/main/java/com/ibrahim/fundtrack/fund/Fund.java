package com.ibrahim.fundtrack.fund;

import jakarta.persistence.*;

@Entity
@Table(name = "fund")
public class Fund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String ticker;

    @Column(nullable = false, length = 150)
    private String name;

    protected Fund() {
    }

    public Fund(String ticker, String name) {
        this.ticker = ticker;
        this.name = name;
    }

    public Long getId() {
        return this.id;
    }

    public String getTicker() {
        return this.ticker;
    }

    public String getName() {
        return this.name;
    }
}
