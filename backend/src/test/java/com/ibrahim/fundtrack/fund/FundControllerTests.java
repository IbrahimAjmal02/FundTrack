package com.ibrahim.fundtrack.fund;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
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
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

// Seed data (Flyway V2) in every fresh test database:
//   fund 1 = MPL100, fund 2 = NRT200, fund 3 = BND300   (ids 1..3, inserted in that order)
//   one NAV per fund for 2026-11-02
// Each test uses its own fund/date combination so tests never depend on each other's data.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class FundControllerTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldReturnSeededFunds() {
        ResponseEntity<String> response = restTemplate.getForEntity("/funds", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<String> tickers = JsonPath.parse(response.getBody()).read("$..ticker");
        assertThat(tickers).contains("MPL100", "NRT200", "BND300");
    }

    @Test
    void shouldCreateNavForFund() {
        CreateNavPriceRequest request = new CreateNavPriceRequest(
                LocalDate.of(2026, 11, 3), new BigDecimal("24.9800"));

        ResponseEntity<String> response = restTemplate.postForEntity("/funds/2/nav", request, String.class);

        // 201 + Location header pointing at /funds/2/nav?date=2026-11-03
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getPath()).isEqualTo("/funds/2/nav");      // part before ?
        assertThat(location.getQuery()).isEqualTo("date=2026-11-03"); // part after ?

        // Body is the NavPriceResponse shape
        DocumentContext json = JsonPath.parse(response.getBody());
        Number fundId = json.read("$.fundId");
        String ticker = json.read("$.ticker");
        String priceDate = json.read("$.priceDate");
        BigDecimal nav = new BigDecimal(json.read("$.nav").toString());

        assertThat(fundId.longValue()).isEqualTo(2L);
        assertThat(ticker).isEqualTo("NRT200");
        assertThat(priceDate).isEqualTo("2026-11-03");
        // compare by VALUE: 24.98 and 24.9800 are equal by compareTo, but not by equals()
        assertThat(nav).isEqualByComparingTo("24.9800");

        // Follow the Location header: it must be a working address that returns the new NAV
        ResponseEntity<String> getResponse = restTemplate.getForEntity(location, String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext list = JsonPath.parse(getResponse.getBody());
        int count = list.read("$.length()");
        assertThat(count).isEqualTo(1);
        assertThat((String) list.read("$[0].priceDate")).isEqualTo("2026-11-03");
        assertThat(new BigDecimal(list.read("$[0].nav").toString())).isEqualByComparingTo("24.98");
    }

    @Test
    void shouldReturnNavHistoryForFundNewestFirst() {
        // Fund 3 (BND300): seeded NAV on 2026-11-02, plus one we add for 2026-11-05
        restTemplate.postForEntity("/funds/3/nav",
                new CreateNavPriceRequest(LocalDate.of(2026, 11, 5), new BigDecimal("10.0500")), String.class);

        ResponseEntity<String> response = restTemplate.getForEntity("/funds/3/nav", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> dates = JsonPath.parse(response.getBody()).read("$[*].priceDate");
        // ISO dates sort correctly as text, so "newest first" = descending string order
        assertThat(dates).contains("2026-11-05", "2026-11-02");
        assertThat(dates).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void shouldReturnEmptyListWhenFundHasNoNavOnDate() {
        ResponseEntity<String> response = restTemplate.getForEntity("/funds/1/nav?date=2026-10-01", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        int count = JsonPath.parse(response.getBody()).read("$.length()");
        assertThat(count).isZero();
    }

    @Test
    void shouldReturnNotFoundWhenGettingNavsForUnknownFund() {
        ResponseEntity<String> response = restTemplate.getForEntity("/funds/999/nav", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectDuplicateNavForSameFundAndDate() {
        // Fund 1 (MPL100) already has a seeded NAV for 2026-11-02
        CreateNavPriceRequest request = new CreateNavPriceRequest(
                LocalDate.of(2026, 11, 2), new BigDecimal("12.9900"));

        ResponseEntity<String> response = restTemplate.postForEntity("/funds/1/nav", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturnNotFoundForUnknownFund() {
        CreateNavPriceRequest request = new CreateNavPriceRequest(
                LocalDate.of(2026, 11, 3), new BigDecimal("10.0000"));

        ResponseEntity<String> response = restTemplate.postForEntity("/funds/999/nav", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectNonPositiveNav() {
        CreateNavPriceRequest request = new CreateNavPriceRequest(
                LocalDate.of(2026, 11, 4), new BigDecimal("-5"));

        ResponseEntity<String> response = restTemplate.postForEntity("/funds/3/nav", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectFutureDate() {
        // relative to today, so this test never goes stale
        CreateNavPriceRequest request = new CreateNavPriceRequest(
                LocalDate.now().plusDays(1), new BigDecimal("12.0000"));

        ResponseEntity<String> response = restTemplate.postForEntity("/funds/3/nav", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectNavWithMoreThanFourDecimals() {
        CreateNavPriceRequest request = new CreateNavPriceRequest(
                LocalDate.of(2026, 11, 4), new BigDecimal("12.123456"));

        ResponseEntity<String> response = restTemplate.postForEntity("/funds/3/nav", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
