package com.ibrahim.fundtrack.holding;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ibrahim.fundtrack.TestcontainersConfiguration;
import com.ibrahim.fundtrack.client.CreateClientRequest;
import com.ibrahim.fundtrack.fund.CreateNavPriceRequest;
import com.ibrahim.fundtrack.order.OrderType;
import com.ibrahim.fundtrack.order.PlaceOrderRequest;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

// GET /clients/{id}/holdings end to end, against the real database.
// Uses its OWN new client so other test classes' orders can't change the numbers.
// Uses fund 2 (NRT200) for today's NAV: no other test class enters a NAV for fund 2 dated today.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class HoldingsIntegrationTests {

    static final long NORTHERN = 2L; // gets a NAV today (25.0000)
    static final long HARBOUR = 3L;  // no NAV today → orders stay PENDING

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldReturnUnitsAndMarketValueFromSettledOrdersOnly() {
        LocalDate today = LocalDate.now();
        long clientId = createClient("Holdings Tester", "SA-900001");

        // Given: BUY $1000 of Northern, SELL 10 Northern, and a BUY of Harbour that will stay PENDING
        placeOrder(new PlaceOrderRequest(clientId, NORTHERN, OrderType.BUY, new BigDecimal("1000.00"), null));
        placeOrder(new PlaceOrderRequest(clientId, NORTHERN, OrderType.SELL, null, new BigDecimal("10.0000")));
        placeOrder(new PlaceOrderRequest(clientId, HARBOUR, OrderType.BUY, new BigDecimal("300.00"), null));

        // Before processing: nothing is settled → no holdings yet
        assertThat(holdingsJson(clientId).read("$.length()", Integer.class)).isZero();

        // NAV for Northern today, then Process day
        ResponseEntity<String> nav = restTemplate.postForEntity("/funds/" + NORTHERN + "/nav",
                new CreateNavPriceRequest(today, new BigDecimal("25.0000")), String.class);
        assertThat(nav.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        restTemplate.postForEntity("/orders/process?date=" + today, null, String.class);

        // When
        DocumentContext json = holdingsJson(clientId);

        // Then: ONE holding (Northern). Harbour's BUY is still PENDING, so it doesn't count.
        assertThat(json.read("$.length()", Integer.class)).isEqualTo(1);
        assertThat((String) json.read("$[0].ticker")).isEqualTo("NRT200");
        // 1000 ÷ 25 = 40 units bought, 10 sold → 30 units
        assertThat(decimal(json, "$[0].units")).isEqualByComparingTo("30.0000");
        assertThat(decimal(json, "$[0].nav")).isEqualByComparingTo("25.0000");
        assertThat((String) json.read("$[0].navDate")).isEqualTo(today.toString());
        // 30 × 25.00 = 750.00
        assertThat(decimal(json, "$[0].marketValue")).isEqualByComparingTo("750.00");
    }

    @Test
    void shouldReturnEmptyListForClientWithNoOrders() {
        long clientId = createClient("No Orders Yet", "SA-900002");

        DocumentContext json = holdingsJson(clientId);

        assertThat(json.read("$.length()", Integer.class)).isZero();
    }

    @Test
    void shouldReturnNotFoundForUnknownClient() {
        ResponseEntity<String> response = restTemplate.getForEntity("/clients/999999/holdings", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long createClient(String name, String subaccountNumber) {
        ResponseEntity<String> response = restTemplate.postForEntity("/clients",
                new CreateClientRequest(name, subaccountNumber), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number id = JsonPath.parse(response.getBody()).read("$.id");
        return id.longValue();
    }

    private void placeOrder(PlaceOrderRequest request) {
        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private DocumentContext holdingsJson(long clientId) {
        ResponseEntity<String> response = restTemplate.getForEntity("/clients/" + clientId + "/holdings",
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JsonPath.parse(response.getBody());
    }

    private static BigDecimal decimal(DocumentContext json, String path) {
        return new BigDecimal(json.read(path).toString());
    }
}
