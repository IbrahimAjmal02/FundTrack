package com.ibrahim.fundtrack.order;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.ibrahim.fundtrack.client.Client;
import com.ibrahim.fundtrack.client.ClientRepository;
import com.ibrahim.fundtrack.fund.Fund;
import com.ibrahim.fundtrack.fund.FundRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final FundOrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final FundRepository fundRepository;

    public OrderController(FundOrderRepository orderRepository,
            ClientRepository clientRepository,
            FundRepository fundRepository) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.fundRepository = fundRepository;
    }

    // POST /orders  { "clientId": 1, "fundId": 1, "orderType": "BUY", "amount": 500.00 }
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request,
            UriComponentsBuilder ucb) {
        Optional<Client> maybeClient = clientRepository.findById(request.clientId());
        Optional<Fund> maybeFund = fundRepository.findById(request.fundId());

        // The URL (/orders) exists; it's the BODY that points at something missing → 400, not 404
        if (maybeClient.isEmpty() || maybeFund.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Client client = maybeClient.get();
        Fund fund = maybeFund.get();
        LocalDate today = LocalDate.now(); // server-side date: prevents late trading

        FundOrder order = switch (request.orderType()) {
            case BUY -> FundOrder.buy(client, fund, request.amount(), today);
            case SELL -> FundOrder.sell(client, fund, request.units(), today);
        };

        FundOrder saved = orderRepository.save(order);

        URI location = ucb.path("/orders/{id}").buildAndExpand(saved.getId()).toUri();

        return ResponseEntity.created(location).body(OrderResponse.from(saved));
    }

    // GET /orders                  → all orders, oldest first
    // GET /orders?status=PENDING   → only that status
    @GetMapping
    public List<OrderResponse> findOrders(@RequestParam(required = false) OrderStatus status) {
        List<FundOrder> orders = (status == null)
                ? orderRepository.findAllByOrderByCreatedAtAsc()
                : orderRepository.findByStatusOrderByCreatedAtAsc(status);

        return orders.stream().map(OrderResponse::from).toList();
    }

    // GET /orders/7 → one order (what the POST's Location header points at)
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        Optional<FundOrder> maybeOrder = orderRepository.findWithClientAndFundById(id);

        if (maybeOrder.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(OrderResponse.from(maybeOrder.get()));
    }
}
