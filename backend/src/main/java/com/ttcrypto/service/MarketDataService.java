package com.ttcrypto.service;

import com.ttcrypto.dto.market.CandlestickDto;
import com.ttcrypto.dto.market.OrderBookDto;
import com.ttcrypto.dto.market.PriceTickDto;
import com.ttcrypto.entity.Order;
import com.ttcrypto.entity.OrderSide;
import com.ttcrypto.entity.OrderStatus;
import com.ttcrypto.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MarketDataService {

    @Autowired
    private OrderRepository orderRepository;

    // Simulated market prices (in production, these would come from external APIs)
    private static final Map<String, BigDecimal> MARKET_PRICES = new HashMap<>();

    static {
        MARKET_PRICES.put("BTC/USDT", new BigDecimal("42500.00"));
        MARKET_PRICES.put("ETH/USDT", new BigDecimal("2300.00"));
        MARKET_PRICES.put("BNB/USDT", new BigDecimal("350.00"));
        MARKET_PRICES.put("XRP/USDT", new BigDecimal("2.50"));
        MARKET_PRICES.put("ADA/USDT", new BigDecimal("1.05"));
    }

    public BigDecimal getCurrentPrice(String tradingPair) {
        return MARKET_PRICES.getOrDefault(tradingPair, new BigDecimal("100.00"));
    }

    public void updatePrice(String tradingPair, BigDecimal price) {
        MARKET_PRICES.put(tradingPair, price);
        log.info("Price updated: {} = {}", tradingPair, price);
    }

    public void simulatePriceFluctuation(String tradingPair) {
        BigDecimal currentPrice = getCurrentPrice(tradingPair);
        double change = (Math.random() - 0.5) * 2; // -1 to +1
        double fluctuation = change / 100; // 0.01% to -0.01% change
        BigDecimal newPrice = currentPrice.multiply(
                new BigDecimal(1 + fluctuation)).setScale(2, RoundingMode.HALF_UP);
        updatePrice(tradingPair, newPrice);
    }

    public OrderBookDto getOrderBook(String tradingPair) {
        List<OrderStatus> activeStatuses = Arrays.asList(OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED);
        List<Order> buyOrders = orderRepository.findByTradingPairAndSideAndStatusesOrderByPrice(
            tradingPair, OrderSide.BUY, activeStatuses);
        List<Order> sellOrders = orderRepository.findByTradingPairAndSideAndStatusesOrderByPrice(
            tradingPair, OrderSide.SELL, activeStatuses);

        // Group by price and sum quantities
        Map<BigDecimal, BigDecimal> buyLevels = new TreeMap<>(Comparator.reverseOrder());
        Map<BigDecimal, BigDecimal> sellLevels = new TreeMap<>();

        buyOrders.forEach(order -> {
            BigDecimal remaining = order.getQuantity().subtract(order.getFilledQuantity());
            buyLevels.merge(order.getPrice(), remaining, BigDecimal::add);
        });

        sellOrders.forEach(order -> {
            BigDecimal remaining = order.getQuantity().subtract(order.getFilledQuantity());
            sellLevels.merge(order.getPrice(), remaining, BigDecimal::add);
        });

        List<OrderBookDto.OrderBookLevel> bids = buyLevels.entrySet().stream()
                .map(e -> OrderBookDto.OrderBookLevel.builder()
                        .price(e.getKey())
                        .quantity(e.getValue())
                        .build())
                .collect(Collectors.toList());

        List<OrderBookDto.OrderBookLevel> asks = sellLevels.entrySet().stream()
                .map(e -> OrderBookDto.OrderBookLevel.builder()
                        .price(e.getKey())
                        .quantity(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return OrderBookDto.builder()
                .tradingPair(tradingPair)
                .bids(bids)
                .asks(asks)
                .lastUpdateTime(System.currentTimeMillis())
                .build();
    }

    public List<CandlestickDto> getCandlesticks(String tradingPair, String interval, int limit) {
        List<CandlestickDto> candlesticks = new ArrayList<>();
        BigDecimal basePrice = getCurrentPrice(tradingPair);
        long now = System.currentTimeMillis();
        long intervalMs = getIntervalMs(interval);
        int safeLimit = Math.max(20, Math.min(limit, 300));

        // Generate a coherent random-walk series so candles look realistic across timeframes.
        BigDecimal lastClose = basePrice;
        double trendBias = (Math.random() - 0.5) * 0.003;

        for (int i = safeLimit - 1; i >= 0; i--) {
            long openTime = now - (i * intervalMs);
            long closeTime = openTime + intervalMs;

            double intervalVolatility = getIntervalVolatility(interval);
            double noise = (Math.random() - 0.5) * 2.0 * intervalVolatility;

            BigDecimal open = lastClose;
            BigDecimal close = open.multiply(BigDecimal.valueOf(1.0 + trendBias + noise));

            BigDecimal wickUpFactor = BigDecimal.valueOf(1.0 + Math.random() * (intervalVolatility * 1.8));
            BigDecimal wickDownFactor = BigDecimal.valueOf(1.0 - Math.random() * (intervalVolatility * 1.8));
            BigDecimal high = open.max(close).multiply(wickUpFactor);
            BigDecimal low = open.min(close).multiply(wickDownFactor);

            // Keep low positive and sane.
            if (low.compareTo(BigDecimal.ONE) < 0) {
                low = BigDecimal.ONE;
            }

            BigDecimal volumeBase = BigDecimal.valueOf(50 + Math.random() * 900);
            BigDecimal volume = volumeBase.multiply(BigDecimal.valueOf(1.0 + intervalVolatility * 40));

            candlesticks.add(CandlestickDto.builder()
                    .tradingPair(tradingPair)
                    .openTime(openTime)
                    .closeTime(closeTime)
                    .open(open.setScale(2, RoundingMode.HALF_UP))
                    .high(high.setScale(2, RoundingMode.HALF_UP))
                    .low(low.setScale(2, RoundingMode.HALF_UP))
                    .close(close.setScale(2, RoundingMode.HALF_UP))
                    .volume(volume.setScale(2, RoundingMode.HALF_UP))
                    .interval(interval)
                    .build());

            lastClose = close;

            // Slightly evolve trend to avoid flat/too-random sequences.
            trendBias = (trendBias * 0.92) + ((Math.random() - 0.5) * intervalVolatility * 0.18);
        }

        return candlesticks;
    }

    private double getIntervalVolatility(String interval) {
        switch (interval.toLowerCase()) {
            case "1m":
                return 0.0012;
            case "5m":
                return 0.0019;
            case "15m":
                return 0.0025;
            case "30m":
                return 0.0032;
            case "1h":
                return 0.004;
            case "4h":
                return 0.0065;
            case "1d":
                return 0.010;
            case "1w":
                return 0.016;
            default:
                return 0.0035;
        }
    }

    private long getIntervalMs(String interval) {
        switch (interval.toLowerCase()) {
            case "1m":
                return 60 * 1000L;
            case "5m":
                return 5 * 60 * 1000L;
            case "30m":
                return 30 * 60 * 1000L;
            case "15m":
                return 15 * 60 * 1000L;
            case "1h":
                return 60 * 60 * 1000L;
            case "4h":
                return 4 * 60 * 60 * 1000L;
            case "1d":
                return 24 * 60 * 60 * 1000L;
            case "1w":
                return 7 * 24 * 60 * 60 * 1000L;
            default:
                return 60 * 1000L;
        }
    }

    public PriceTickDto getPriceTick(String tradingPair) {
        BigDecimal price = getCurrentPrice(tradingPair);
        return PriceTickDto.builder()
                .tradingPair(tradingPair)
                .price(price)
                .bidPrice(price.multiply(new BigDecimal("0.99")))
                .askPrice(price.multiply(new BigDecimal("1.01")))
                .quantity(new BigDecimal(Math.random() * 100))
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
