package com.ibrahim.fundtrack.fund;

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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/funds")
public class FundController {
    private final FundRepository fundRepository;
    private final NavPriceRepository navPriceRepository;

    public FundController(FundRepository fundRepository, NavPriceRepository navPriceRepository) {
        this.fundRepository = fundRepository;
        this.navPriceRepository = navPriceRepository;
    }

    @GetMapping
    public List<Fund> findAll() {
        return fundRepository.findAll();
    }

    // POST /funds/{id}/nav e.g. POST /funds/2/nav { "priceDate": "2026-11-03",
    // "nav": 24.98 }
    @PostMapping("/{id}/nav")
    public ResponseEntity<NavPriceResponse> createNav(@PathVariable Long id,
            @Valid @RequestBody CreateNavPriceRequest request,
            UriComponentsBuilder ucb) {
        Optional<Fund> maybeFund = fundRepository.findById(id);

        if (maybeFund.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Fund fund = maybeFund.get(); // safe: the 404 check above guarantees a fund

        if (navPriceRepository.existsByFundAndPriceDate(fund, request.priceDate())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        NavPrice saved = navPriceRepository.save(new NavPrice(fund, request.priceDate(), request.nav()));

        URI location = ucb.path("/funds/{id}/nav/{date}")
                .buildAndExpand(fund.getId(), saved.getPriceDate())
                .toUri();

        return ResponseEntity.created(location).body(NavPriceResponse.from(saved));
    }
}
