package com.ibrahim.fundtrack.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ibrahim.fundtrack.TestcontainersConfiguration;
import com.ibrahim.fundtrack.fund.CreateNavPriceRequest;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

// "Process day" end to end: HTTP → controller → @Transactional service → REAL Postgres.
// The unit tests (OrderProcessingServiceTests) use Mockito fakes; these use the real database,
// which catches things fakes can't (e.g. Hibernate auto-flushing settled orders mid-run).
//
// Test data is chosen so other test classes can't interfere:
//   client 3 (Maria) is only used here; fund 1 gets TODAY's NAV here; fund 3 never gets one today.
// Assertions check specific orders by id, never total counts (other classes may also place
// orders dated today, and Process day processes ALL of them).
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderProcessingIntegrationTests {

    static final long MARIA = 3L;
    static final long MAPLE = 1L;    // gets a NAV today (10.0000)
    static final long HARBOUR = 3L;  // never gets a NAV today

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldProcessTheDayAgainstTheRealDatabase() {
        LocalDate today = LocalDate.now();

        // Given (placed in this order, so processed oldest-first in this order):
        long buy = placeBuy(MARIA, MAPLE, "1000.00");   // 1000 ÷ 10 = 100 units
        long sell1 = placeSell(MARIA, MAPLE, "40.0000"); // 100 → 60 left
        long sell2 = placeSell(MARIA, MAPLE, "40.0000"); // 60 → 20 left
        long sell3 = placeSell(MARIA, MAPLE, "40.0000"); // only 20 left → REJECTED
        long noNav = placeBuy(MARIA, HARBOUR, "250.00");  // no NAV today → stays PENDING

        // Operations enters today's NAV for Maple only
        ResponseEntity<String> nav = restTemplate.postForEntity("/funds/" + MAPLE + "/nav",
                new CreateNavPriceRequest(today, new BigDecimal("10.0000")), String.class);
        assertThat(nav.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // When
        ResponseEntity<String> result = restTemplate.postForEntity("/orders/process?date=" + today, null,
                String.class);

        // Then: the endpoint answers with a summary for that date
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) JsonPath.parse(result.getBody()).read("$.date")).isEqualTo(today.toString());

        // The BUY settled: 100 units at 10.00
        DocumentContext b = getOrder(buy);
        assertThat((String) b.read("$.status")).isEqualTo("SETTLED");
        assertThat(decimal(b, "$.unitsSettled")).isEqualByComparingTo("100.0000");
        assertThat(decimal(b, "$.priceUsed")).isEqualByComparingTo("10.0000");

        // The first two SELLs settled: 40 × 10 = $400.00 each.
        // These depend on the same-day BUY being counted and on units NOT being double-counted
        // when Hibernate auto-flushes the settled orders before the holdings query.
        DocumentContext s1 = getOrder(sell1);
        assertThat((String) s1.read("$.status")).isEqualTo("SETTLED");
        assertThat(decimal(s1, "$.amount")).isEqualByComparingTo("400.00");
        assertThat((String) getOrder(sell2).read("$.status")).isEqualTo("SETTLED");

        // The third SELL needed 40 but only 20 were left
        DocumentContext s3 = getOrder(sell3);
        assertThat((String) s3.read("$.status")).isEqualTo("REJECTED");
        assertThat((Object) s3.read("$.priceUsed")).isNull();

        // No NAV for Harbour today → still waiting
        assertThat((String) getOrder(noNav).read("$.status")).isEqualTo("PENDING");

        // Running it again is safe: settled/rejected orders are final and aren't touched
        restTemplate.postForEntity("/orders/process?date=" + today, null, String.class);
        assertThat((String) getOrder(sell1).read("$.status")).isEqualTo("SETTLED");
        assertThat((String) getOrder(sell3).read("$.status")).isEqualTo("REJECTED");
    }

    @Test
    void shouldRequireADateToProcess() {
        ResponseEntity<String> response = restTemplate.postForEntity("/orders/process", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long placeBuy(long clientId, long fundId, String amount) {
        return place(new PlaceOrderRequest(clientId, fundId, OrderType.BUY, new BigDecimal(amount), null));
    }

    private long placeSell(long clientId, long fundId, String units) {
        return place(new PlaceOrderRequest(clientId, fundId, OrderType.SELL, null, new BigDecimal(units)));
    }

    private long place(PlaceOrderRequest request) {
        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number id = JsonPath.parse(response.getBody()).read("$.id");
        return id.longValue();
    }

    private DocumentContext getOrder(long id) {
        ResponseEntity<String> response = restTemplate.getForEntity("/orders/" + id, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    private static BigDecimal decimal(DocumentContext json, String path) {
        return new BigDecimal(json.read(path).toString());
    }
}
