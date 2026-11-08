package com.ibrahim.fundtrack.order;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundOrderRepository extends JpaRepository<FundOrder, Long> {

    // GET /orders?status=PENDING → WHERE status = 'PENDING' ORDER BY created_at
    // @EntityGraph loads each order's client AND fund in the same query (JOINs),
    // because the response shows the client's name and the fund's ticker.
    // Without it: LazyInitializationException (open-in-view is off), or N+1
    // queries.
    @EntityGraph(attributePaths = { "client", "fund" })
    List<FundOrder> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    // "Process day": that date's orders with a given status (PENDING), oldest
    // first.
    // → WHERE status = ? AND order_date = ? ORDER BY created_at
    // Postgres uses idx_fund_order_status_date (from V1) for this automatically.
    @EntityGraph(attributePaths = { "client", "fund" })
    List<FundOrder> findByStatusAndOrderDateOrderByCreatedAtAsc(OrderStatus status, LocalDate orderDate);

    // GET /orders/{id}: like findById, but loads client + fund too (for OrderResponse).
    // Spring Data ignores the words between "find" and "By", so "WithClientAndFund" is
    // just a readable label; the query is still WHERE id = ?
    @EntityGraph(attributePaths = { "client", "fund" })
    Optional<FundOrder> findWithClientAndFundById(Long id);

    // GET /orders (no filter): every order, oldest first
    @EntityGraph(attributePaths = { "client", "fund" })
    List<FundOrder> findAllByOrderByCreatedAtAsc();

}
