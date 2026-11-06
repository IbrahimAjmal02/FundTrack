package com.ibrahim.fundtrack.client;

import jakarta.persistence.*;

@Entity
@Table(name = "client")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "subaccount_number", nullable = false, unique = true, length = 20)
    private String subaccountNumber;

    protected Client() {
    }

    public Client(String name, String subaccountNumber) {
        this.name = name;
        this.subaccountNumber = subaccountNumber;
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getSubaccountNumber() {
        return this.subaccountNumber;
    }
}
