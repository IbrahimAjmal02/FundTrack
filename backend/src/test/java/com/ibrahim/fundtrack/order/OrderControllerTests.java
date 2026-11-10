package com.ibrahim.fundtrack.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ibrahim.fundtrack.TestcontainersConfiguration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

// Integration tests for placing and reading orders (POST /orders, GET /orders, GET /orders/{id}).
// Seed data: client 1 = Sarah, 2 = Kumar, 3 = Maria; fund 1 = MPL100, 2 = NRT200, 3 = BND300.
// These tests only PLACE orders; processing is covered by OrderProcessingIntegrationTests.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderControllerTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldPlaceBuyOrderAsPendingDatedToday() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L, 1L, OrderType.BUY, new BigDecimal("500.00"), null);

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentContext json = JsonPath.parse(response.getBody());
        assertThat((String) json.read("$.status")).isEqualTo("PENDING");
        assertThat((String) json.read("$.orderType")).isEqualTo("BUY");
        assertThat((String) json.read("$.clientName")).isEqualTo("Sarah Chen");
        assertThat((String) json.read("$.ticker")).isEqualTo("MPL100");
        assertThat(new BigDecimal(json.read("$.amount").toString())).isEqualByComparingTo("500.00");
        assertThat((Object) json.read("$.units")).isNull();
        assertThat((Object) json.read("$.priceUsed")).isNull();     // not priced until Process day
        assertThat((Object) json.read("$.unitsSettled")).isNull();
        // The SERVER sets the date (prevents late trading)
        assertThat((String) json.read("$.orderDate")).isEqualTo(LocalDate.now().toString());

        // Location header works: GET /orders/{id} returns the same order
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();
        ResponseEntity<String> getResponse = restTemplate.getForEntity(location, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Number id = json.read("$.id");
        Number fetchedId = JsonPath.parse(getResponse.getBody()).read("$.id");
        assertThat(fetchedId.longValue()).isEqualTo(id.longValue());
    }

    @Test
    void shouldPlaceSellOrderWithUnitsAndNoAmount() {
        PlaceOrderRequest request = new PlaceOrderRequest(2L, 2L, OrderType.SELL, null, new BigDecimal("20.0000"));

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DocumentContext json = JsonPath.parse(response.getBody());
        assertThat((String) json.read("$.status")).isEqualTo("PENDING");
        assertThat((String) json.read("$.orderType")).isEqualTo("SELL");
        assertThat(new BigDecimal(json.read("$.units").toString())).isEqualByComparingTo("20.0000");
        assertThat((Object) json.read("$.amount")).isNull(); // worked out at settlement
    }

    @Test
    void shouldRejectBuyWithoutAmount() {
        // @AssertTrue cross-field rule: a BUY needs an amount
        PlaceOrderRequest request = new PlaceOrderRequest(1L, 1L, OrderType.BUY, null, new BigDecimal("10"));

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectOrderWithBothAmountAndUnits() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L, 1L, OrderType.BUY,
                new BigDecimal("100.00"), new BigDecimal("5"));

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L, 1L, OrderType.BUY, new BigDecimal("-50.00"), null);

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectUnknownOrderType() {
        // A Map, because the record can't hold an invalid enum value
        Map<String, Object> body = Map.of("clientId", 1, "fundId", 1, "orderType", "HOLD", "amount", 100.00);

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectUnknownClient() {
        PlaceOrderRequest request = new PlaceOrderRequest(999L, 1L, OrderType.BUY, new BigDecimal("100.00"), null);

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectUnknownFund() {
        PlaceOrderRequest request = new PlaceOrderRequest(1L, 999L, OrderType.BUY, new BigDecimal("100.00"), null);

        ResponseEntity<String> response = restTemplate.postForEntity("/orders", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldListOrdersFilteredByStatus() {
        // Place one so the list is never empty, whatever order the tests run in
        PlaceOrderRequest request = new PlaceOrderRequest(1L, 1L, OrderType.BUY, new BigDecimal("75.00"), null);
        Number id = JsonPath.parse(restTemplate.postForEntity("/orders", request, String.class).getBody())
                .read("$.id");

        ResponseEntity<String> response = restTemplate.getForEntity("/orders?status=PENDING", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext json = JsonPath.parse(response.getBody());
        List<String> statuses = json.read("$[*].status");
        List<Number> ids = json.read("$[*].id");
        assertThat(statuses).isNotEmpty().allMatch("PENDING"::equals); // only PENDING comes back
        assertThat(ids).anyMatch(i -> i.longValue() == id.longValue());
    }

    @Test
    void shouldListOnlyOneClientsOrders() {
        // One order for Kumar (client 2), one for Maria (client 3).
        // Fund 3 has no NAV today, so these stay PENDING and can't affect the Process-day tests.
        PlaceOrderRequest kumars = new PlaceOrderRequest(2L, 3L, OrderType.BUY, new BigDecimal("60.00"), null);
        PlaceOrderRequest marias = new PlaceOrderRequest(3L, 3L, OrderType.BUY, new BigDecimal("70.00"), null);
        Number kumarOrderId = JsonPath.parse(restTemplate.postForEntity("/orders", kumars, String.class).getBody())
                .read("$.id");
        Number mariaOrderId = JsonPath.parse(restTemplate.postForEntity("/orders", marias, String.class).getBody())
                .read("$.id");

        ResponseEntity<String> response = restTemplate.getForEntity("/orders?clientId=2", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext json = JsonPath.parse(response.getBody());
        List<Number> clientIds = json.read("$[*].clientId");
        List<Number> ids = json.read("$[*].id");
        assertThat(clientIds).isNotEmpty().allMatch(c -> c.longValue() == 2L); // only Kumar's
        assertThat(ids).anyMatch(i -> i.longValue() == kumarOrderId.longValue());
        assertThat(ids).noneMatch(i -> i.longValue() == mariaOrderId.longValue());
    }

    @Test
    void shouldReturnEmptyListForClientWithNoOrders() {
        ResponseEntity<String> response = restTemplate.getForEntity("/orders?clientId=999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JsonPath.parse(response.getBody()).read("$.length()", Integer.class)).isZero();
    }

    @Test
    void shouldRejectInvalidStatusFilter() {
        ResponseEntity<String> response = restTemplate.getForEntity("/orders?status=BANANA", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnNotFoundForUnknownOrder() {
        ResponseEntity<String> response = restTemplate.getForEntity("/orders/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
