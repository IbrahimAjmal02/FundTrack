package com.ibrahim.fundtrack.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ibrahim.fundtrack.client.Client;
import com.ibrahim.fundtrack.fund.Fund;
import com.ibrahim.fundtrack.holding.UnitsByFund;

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

    // Units a client currently owns in one fund = settled BUY units − settled SELL units.
    // A derived name can't express SUM/CASE, so this is a hand-written JPQL query.
    // JPQL uses ENTITY and FIELD names (FundOrder, orderType), not table/column names.
    // COALESCE(..., 0): if there are no settled orders, SUM is null → return 0 instead.
    @Query("""
            SELECT COALESCE(SUM(
                       CASE WHEN o.orderType = com.ibrahim.fundtrack.order.OrderType.BUY
                            THEN o.unitsSettled
                            ELSE -o.unitsSettled
                       END), 0)
            FROM FundOrder o
            WHERE o.client = :client
              AND o.fund = :fund
              AND o.status = com.ibrahim.fundtrack.order.OrderStatus.SETTLED
            """)
    BigDecimal unitsHeld(@Param("client") Client client, @Param("fund") Fund fund);

    // Holdings: the same SUM as unitsHeld, but for ALL of a client's funds at once.
    //   GROUP BY → one result row per fund (instead of one grand total)
    //   HAVING   → like WHERE, but filters the GROUPED rows: drop funds where they now hold 0
    //   SELECT new ...UnitsByFund(...) → JPQL builds a UnitsByFund record from each row
    @Query("""
            SELECT new com.ibrahim.fundtrack.holding.UnitsByFund(
                       f.id, f.ticker, f.name,
                       SUM(CASE WHEN o.orderType = com.ibrahim.fundtrack.order.OrderType.BUY
                                THEN o.unitsSettled
                                ELSE -o.unitsSettled
                           END))
            FROM FundOrder o
            JOIN o.fund f
            WHERE o.client = :client
              AND o.status = com.ibrahim.fundtrack.order.OrderStatus.SETTLED
            GROUP BY f.id, f.ticker, f.name
            HAVING SUM(CASE WHEN o.orderType = com.ibrahim.fundtrack.order.OrderType.BUY
                            THEN o.unitsSettled
                            ELSE -o.unitsSettled
                       END) <> 0
            ORDER BY f.ticker
            """)
    List<UnitsByFund> unitsByFund(@Param("client") Client client);
}
