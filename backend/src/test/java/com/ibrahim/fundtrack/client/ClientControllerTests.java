package com.ibrahim.fundtrack.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ClientControllerTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldReturnSeededClients() {
        ResponseEntity<String> response = restTemplate.getForEntity("/clients", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext json = JsonPath.parse(response.getBody());
        List<String> subaccounts = json.read("$..subaccountNumber");
        assertThat(subaccounts).contains("SA-100001", "SA-100002", "SA-100003");
    }

    @Test
    void shouldCreateNewClient() {
        // Arrange: the request body (Jackson turns this record into JSON)
        CreateClientRequest request = new CreateClientRequest("Ali Khan", "SA-100004");

        // Act: POST /clients
        ResponseEntity<String> response = restTemplate.postForEntity("/clients", request, String.class);

        // Assert: 201 Created + Location header
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();

        // Assert: the response body is the saved client, with a DB-generated id
        DocumentContext json = JsonPath.parse(response.getBody());
        Number id = json.read("$.id");
        String name = json.read("$.name");
        String subaccountNumber = json.read("$.subaccountNumber");
        assertThat(id).isNotNull();
        assertThat(name).isEqualTo("Ali Khan");
        assertThat(subaccountNumber).isEqualTo("SA-100004");

        // Assert: the Location header points at the new id
        assertThat(location.getPath()).isEqualTo("/clients/" + id);

        // Assert: it was really saved — it now shows up in GET /clients
        ResponseEntity<String> listResponse = restTemplate.getForEntity("/clients", String.class);
        List<String> subaccounts = JsonPath.parse(listResponse.getBody()).read("$..subaccountNumber");
        assertThat(subaccounts).contains("SA-100004");
    }

    @Test
    void shouldRejectDuplicateSubaccountNumber() {
        // SA-100001 already belongs to Sarah Chen (seeded by Flyway V2)
        CreateClientRequest request = new CreateClientRequest("Someone Else", "SA-100001");

        ResponseEntity<String> response = restTemplate.postForEntity("/clients", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Nothing was saved: "Someone Else" must not appear
        List<String> names = JsonPath.parse(restTemplate.getForEntity("/clients", String.class).getBody())
                .read("$..name");
        assertThat(names).doesNotContain("Someone Else");
    }

    @Test
    void shouldRejectBlankName() {
        CreateClientRequest request = new CreateClientRequest("   ", "SA-100005");

        ResponseEntity<String> response = restTemplate.postForEntity("/clients", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // @Valid stopped it before the controller ran, so nothing was saved
        List<String> subaccounts = JsonPath.parse(restTemplate.getForEntity("/clients", String.class).getBody())
                .read("$..subaccountNumber");
        assertThat(subaccounts).doesNotContain("SA-100005");
    }

    @Test
    void shouldRejectInvalidSubaccountFormat() {
        CreateClientRequest request = new CreateClientRequest("Bad Format", "ABC-1");

        ResponseEntity<String> response = restTemplate.postForEntity("/clients", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectMissingSubaccountNumber() {
        // A Map instead of the record, so the field can be left out entirely (→ null)
        Map<String, String> body = Map.of("name", "No Subaccount");

        ResponseEntity<String> response = restTemplate.postForEntity("/clients", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
