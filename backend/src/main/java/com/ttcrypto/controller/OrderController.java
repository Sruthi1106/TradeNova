package com.ttcrypto.controller;

import com.ttcrypto.dto.order.CreateOrderRequest;
import com.ttcrypto.dto.order.OrderDto;
import com.ttcrypto.entity.User;
import com.ttcrypto.exception.ResourceNotFoundException;
import com.ttcrypto.repository.UserRepository;
import com.ttcrypto.service.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        log.info("Creating order for user: {}", authentication.getName());
        // In production, get userId from authentication/JWT token
        Long userId = getUserIdFromAuthentication(authentication);
        OrderDto order = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        log.info("Fetching order: {}", orderId);
        OrderDto order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<Page<OrderDto>> getUserOrders(
            Authentication authentication,
            Pageable pageable) {
        log.info("Fetching orders for user: {}", authentication.getName());
        Long userId = getUserIdFromAuthentication(authentication);
        Page<OrderDto> orders = orderService.getUserOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        log.info("Cancelling order: {} for user: {}", orderId, authentication.getName());
        Long userId = getUserIdFromAuthentication(authentication);
        orderService.cancelOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pair/{tradingPair}")
    public ResponseEntity<?> getTradingPairOrders(@PathVariable String tradingPair) {
        log.info("Fetching orders for trading pair: {}", tradingPair);
        return ResponseEntity.ok(orderService.getTradingPairOrders(tradingPair));
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }

        String identifier = authentication.getName().trim();
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
