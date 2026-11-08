package com.ibrahim.fundtrack.fund;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NavPriceRepository extends JpaRepository<NavPrice, Long> {
    boolean existsByFundAndPriceDate(Fund fund, LocalDate priceDate);

    // All NAVs for a fund, newest first: WHERE fund_id = ? ORDER BY price_date DESC
    // @EntityGraph loads each NAV's Fund in the SAME query (a JOIN), so the response
    // can read fund.getTicker() after the query finishes. Without it, Fund is a lazy
    // placeholder and reading it later throws LazyInitializationException.
    @EntityGraph(attributePaths = "fund")
    List<NavPrice> findByFundOrderByPriceDateDesc(Fund fund);

    // NAVs for a fund on one date (0 or 1 rows, thanks to UNIQUE (fund_id, price_date))
    @EntityGraph(attributePaths = "fund")
    List<NavPrice> findByFundAndPriceDate(Fund fund, LocalDate priceDate);
}
