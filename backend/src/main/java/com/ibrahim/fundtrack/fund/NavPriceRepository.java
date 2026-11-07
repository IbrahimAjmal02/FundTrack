package com.ibrahim.fundtrack.fund;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NavPriceRepository extends JpaRepository<NavPrice, Long> {
    boolean existsByFundAndPriceDate(Fund fund, LocalDate priceDate);
}
