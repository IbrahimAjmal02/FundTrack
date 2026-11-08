package com.ibrahim.fundtrack.client;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.ibrahim.fundtrack.holding.HoldingResponse;
import com.ibrahim.fundtrack.holding.HoldingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/clients")
public class ClientController {
    private final ClientRepository clientRepository;
    private final HoldingService holdingService;

    public ClientController(ClientRepository clientRepository, HoldingService holdingService) {
        this.clientRepository = clientRepository;
        this.holdingService = holdingService;
    }

    @GetMapping
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> findById(@PathVariable Long id) {
        Optional<Client> maybeClient = clientRepository.findById(id);

        if (maybeClient.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Client response = maybeClient.get();

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Client> create(@Valid @RequestBody CreateClientRequest request, UriComponentsBuilder ucb) {
        if (clientRepository.existsBySubaccountNumber(request.subaccountNumber())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Client saved = clientRepository.save(new Client(request.name(), request.subaccountNumber()));

        URI location = ucb.path("/clients/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(saved);
    }

    // GET /clients/1/holdings → what Sarah owns now: units per fund + market value at the latest NAV
    @GetMapping("/{id}/holdings")
    public ResponseEntity<List<HoldingResponse>> holdings(@PathVariable Long id) {
        Optional<Client> maybeClient = clientRepository.findById(id);

        if (maybeClient.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(holdingService.holdingsFor(maybeClient.get()));
    }
}
