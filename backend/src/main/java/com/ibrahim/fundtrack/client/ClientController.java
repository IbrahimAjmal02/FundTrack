package com.ibrahim.fundtrack.client;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import java.net.URI;

@RestController
@RequestMapping("/clients")
public class ClientController {
    private final ClientRepository clientRepository;

    public ClientController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping
    public List<Client> findAll() {
        return clientRepository.findAll();
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

}
