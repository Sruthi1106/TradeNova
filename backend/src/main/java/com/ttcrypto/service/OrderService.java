package com.ttcrypto.service;

import com.ttcrypto.dto.order.CreateOrderRequest;
import com.ttcrypto.dto.order.OrderDto;
import com.ttcrypto.entity.*;
import com.ttcrypto.exception.InsufficientBalanceException;
import com.ttcrypto.exception.InvalidOrderException;
import com.ttcrypto.exception.ResourceNotFoundException;
import com.ttcrypto.repository.OrderRepository;
import com.ttcrypto.repository.TradeRepository;
import com.ttcrypto.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private OrderMatchingService orderMatchingService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private TradeRepository tradeRepository;

    public OrderDto createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateOrder(request, user);

        // Extract quote and base currency
        String[] pair = request.getTradingPair().split("/");
        if (pair.length != 2) {
            throw new InvalidOrderException("Invalid trading pair format");
        }
        String baseCurrency = pair[0];
        String quoteCurrency = pair[1];

        BigDecimal totalValue = request.getQuantity().multiply(
                request.getPrice() != null ? request.getPrice() : getMarketPrice(request.getTradingPair())
        );

        // Lock balance based on order side
        if (request.getSide() == OrderSide.BUY) {
            walletService.lockBalance(userId, quoteCurrency, totalValue);
        } else {
            walletService.lockBalance(userId, baseCurrency, request.getQuantity());
        }

        Order order = Order.builder()
                .user(user)
                .tradingPair(request.getTradingPair())
                .type(request.getType())
                .side(request.getSide())
                .quantity(request.getQuantity())
                .price(request.getPrice() != null ? request.getPrice() : getMarketPrice(request.getTradingPair()))
                .filledQuantity(BigDecimal.ZERO)
                .totalValue(totalValue)
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created: {} {} {}", savedOrder.getId(), request.getSide(), request.getTradingPair());

        // Attempt to match order immediately
        orderMatchingService.matchOrders(request.getTradingPair());

        return mapOrderToDto(orderRepository.save(savedOrder));
    }

    private void validateOrder(CreateOrderRequest request, User user) {
        if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Quantity must be positive");
        }

        if (request.getType() == OrderType.LIMIT && 
            (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new InvalidOrderException("Price is required and must be positive for limit orders");
        }

        // Check wallet availability
        String[] pair = request.getTradingPair().split("/");
        if (pair.length != 2) {
            throw new InvalidOrderException("Invalid trading pair format");
        }
        String baseCurrency = pair[0];
        String quoteCurrency = pair[1];
        
        if (request.getSide() == OrderSide.BUY) {
            BigDecimal totalValue = request.getQuantity().multiply(
                    request.getPrice() != null ? request.getPrice() : getMarketPrice(request.getTradingPair())
            );
            BigDecimal availableBalance = walletService.getAvailableBalance(user.getId(), quoteCurrency);
            if (availableBalance.compareTo(totalValue) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient balance to place buy order. Available: " + availableBalance + 
                        ", Required: " + totalValue
                );
            }
            return;
        }

        BigDecimal availableBaseBalance = walletService.getAvailableBalance(user.getId(), baseCurrency);
        if (availableBaseBalance.compareTo(request.getQuantity()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance to place sell order. Available: " + availableBaseBalance +
                    ", Required: " + request.getQuantity()
            );
        }
    }

    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new InvalidOrderException("You can only cancel your own orders");
        }

        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderException("Cannot cancel a filled or already cancelled order");
        }

        // Unlock balance
        String[] pair = order.getTradingPair().split("/");
        String baseCurrency = pair[0];
        String quoteCurrency = pair[1];

        if (order.getSide() == OrderSide.BUY) {
            BigDecimal executedQuoteAmount = tradeRepository
                    .findByBuyOrderIdOrSellOrderId(order.getId(), -1L)
                    .stream()
                    .map(Trade::getTotalValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal lockedAmount = order.getTotalValue().subtract(executedQuoteAmount);
            if (lockedAmount.compareTo(BigDecimal.ZERO) < 0) {
                lockedAmount = BigDecimal.ZERO;
            }
            walletService.unlockBalance(userId, quoteCurrency, lockedAmount);
        } else {
            BigDecimal lockedAmount = order.getQuantity().subtract(order.getFilledQuantity());
            walletService.unlockBalance(userId, baseCurrency, lockedAmount);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled: {}", orderId);
    }

    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapOrderToDto(order);
    }

    public Page<OrderDto> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapOrderToDto);
    }

    public List<OrderDto> getTradingPairOrders(String tradingPair) {
        return orderRepository.findByTradingPairAndStatusOrderByCreatedAtDesc(tradingPair, OrderStatus.PENDING)
                .stream()
                .map(this::mapOrderToDto)
                .collect(Collectors.toList());
    }

    private BigDecimal getMarketPrice(String tradingPair) {
        return marketDataService.getCurrentPrice(tradingPair);
    }

    private OrderDto mapOrderToDto(Order order) {
        return OrderDto.builder()
                .id(order.getId())
                .tradingPair(order.getTradingPair())
                .type(order.getType())
                .side(order.getSide())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .filledQuantity(order.getFilledQuantity())
                .totalValue(order.getTotalValue())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
