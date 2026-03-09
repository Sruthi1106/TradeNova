package com.ttcrypto.service;

import com.ttcrypto.entity.*;
import com.ttcrypto.repository.OrderRepository;
import com.ttcrypto.repository.TradeRepository;
import com.ttcrypto.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional
public class OrderMatchingService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletService walletService;

    public void matchOrders(String tradingPair) {
        List<Order> buyOrders = orderRepository.findBuyOrders(tradingPair);
        List<Order> sellOrders = orderRepository.findSellOrders(tradingPair);

        for (Order buyOrder : buyOrders) {
            if (!buyOrder.getStatus().equals(OrderStatus.PENDING)) {
                continue;
            }

            BigDecimal buyQuantityRemaining = buyOrder.getQuantity()
                    .subtract(buyOrder.getFilledQuantity());

            for (Order sellOrder : sellOrders) {
                if (buyQuantityRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                if (!(sellOrder.getStatus().equals(OrderStatus.PENDING)
                        || sellOrder.getStatus().equals(OrderStatus.PARTIALLY_FILLED))) {
                    continue;
                }

                if (isBuyerAccepts(buyOrder, sellOrder)) {
                    BigDecimal sellQuantityRemaining = sellOrder.getQuantity()
                            .subtract(sellOrder.getFilledQuantity());
                    BigDecimal matchedQuantity = buyQuantityRemaining
                            .min(sellQuantityRemaining);

                    executeTrade(buyOrder, sellOrder, matchedQuantity);

                    buyQuantityRemaining = buyQuantityRemaining.subtract(matchedQuantity);

                    // Update sell order
                    sellOrder.setFilledQuantity(sellOrder.getFilledQuantity().add(matchedQuantity));
                    if (sellOrder.getFilledQuantity().compareTo(sellOrder.getQuantity()) >= 0) {
                        sellOrder.setStatus(OrderStatus.FILLED);
                        releaseResidualLockedFundsIfNeeded(sellOrder);
                    } else {
                        sellOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
                    }
                    orderRepository.save(sellOrder);
                }
            }

            // Update buy order
            buyOrder.setFilledQuantity(buyOrder.getQuantity().subtract(buyQuantityRemaining));
            if (buyOrder.getFilledQuantity().compareTo(buyOrder.getQuantity()) >= 0) {
                buyOrder.setStatus(OrderStatus.FILLED);
                releaseResidualLockedFundsIfNeeded(buyOrder);
            } else if (buyOrder.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0) {
                buyOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
            }
            orderRepository.save(buyOrder);
        }
    }

    private boolean isBuyerAccepts(Order buyOrder, Order sellOrder) {
        if (buyOrder.getType() == OrderType.MARKET || sellOrder.getType() == OrderType.MARKET) {
            return true;
        }
        return buyOrder.getPrice().compareTo(sellOrder.getPrice()) >= 0;
    }

    private void executeTrade(Order buyOrder, Order sellOrder, BigDecimal quantity) {
        BigDecimal tradePrice;
        if (sellOrder.getType() == OrderType.MARKET && buyOrder.getType() == OrderType.MARKET) {
            tradePrice = buyOrder.getPrice();
        } else if (sellOrder.getType() == OrderType.MARKET) {
            tradePrice = buyOrder.getPrice();
        } else {
            tradePrice = sellOrder.getPrice();
        }

        BigDecimal totalValue = quantity.multiply(tradePrice);

        // Create Trade record
        Trade trade = Trade.builder()
                .tradingPair(buyOrder.getTradingPair())
                .buyer(buyOrder.getUser())
                .seller(sellOrder.getUser())
                .buyOrder(buyOrder)
                .sellOrder(sellOrder)
                .quantity(quantity)
                .price(tradePrice)
                .totalValue(totalValue)
                .build();

        Trade savedTrade = tradeRepository.save(trade);
        log.info("Trade executed: {} {} at price {}", quantity, buyOrder.getTradingPair(), tradePrice);

        // Extract quote and base currency from trading pair (e.g., "BTC/USDT")
        String[] pair = buyOrder.getTradingPair().split("/");
        String baseCurrency = pair[0];
        String quoteCurrency = pair[1];

        // Settle against already locked funds from both orders.
        walletService.consumeLockedBalance(buyOrder.getUser().getId(), quoteCurrency, totalValue);
        walletService.consumeLockedBalance(sellOrder.getUser().getId(), baseCurrency, quantity);

        // Buyer receives base currency, seller receives quote currency.
        walletService.addBalance(buyOrder.getUser().getId(), baseCurrency, quantity);
        walletService.addBalance(sellOrder.getUser().getId(), quoteCurrency, totalValue);

        // Record transactions
        recordTransaction(buyOrder.getUser().getId(), quoteCurrency, 
                TransactionType.BUY, totalValue, savedTrade.getId());
        recordTransaction(sellOrder.getUser().getId(), quoteCurrency,
                TransactionType.SELL, totalValue, savedTrade.getId());
    }

    private void recordTransaction(Long userId, String currency, TransactionType type, 
                                   BigDecimal amount, Long tradeId) {
        Wallet wallet = walletService.getWallet(userId, currency);
        
        User user = new User();
        user.setId(userId);
        
        Transaction transaction = Transaction.builder()
                .user(user)
                .currency(currency)
                .transactionType(type)
                .amount(amount)
                .balanceBefore(wallet.getBalance().add(amount))
                .balanceAfter(wallet.getBalance())
                .relatedTradeId(tradeId)
                .description("Trade execution: " + type.getDescription())
                .build();

        transactionRepository.save(transaction);
    }

    private void releaseResidualLockedFundsIfNeeded(Order order) {
        String[] pair = order.getTradingPair().split("/");
        String baseCurrency = pair[0];
        String quoteCurrency = pair[1];

        if (order.getSide() == OrderSide.BUY) {
            BigDecimal executedQuoteAmount = tradeRepository
                    .findByBuyOrderIdOrSellOrderId(order.getId(), -1L)
                    .stream()
                    .map(Trade::getTotalValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal residual = order.getTotalValue().subtract(executedQuoteAmount);
            if (residual.compareTo(BigDecimal.ZERO) > 0) {
                walletService.unlockBalance(order.getUser().getId(), quoteCurrency, residual);
            }
            return;
        }

        BigDecimal residual = order.getQuantity().subtract(order.getFilledQuantity());
        if (residual.compareTo(BigDecimal.ZERO) > 0) {
            walletService.unlockBalance(order.getUser().getId(), baseCurrency, residual);
        }
    }
}
