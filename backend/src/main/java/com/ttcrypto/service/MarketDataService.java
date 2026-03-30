package com.ttcrypto.service;

import com.ttcrypto.dto.market.CandlestickDto;
import com.ttcrypto.dto.market.OrderBookDto;
import com.ttcrypto.dto.market.PriceTickDto;
import com.ttcrypto.entity.Order;
import com.ttcrypto.entity.OrderSide;
import com.ttcrypto.entity.OrderStatus;
import com.ttcrypto.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Service
@Slf4j
public class MarketDataService {

    @Autowired
    private OrderRepository orderRepository;

    private static final String COINBASE_BASE_URL = "https://api.coinbase.com/v2";
    private static final long PRICE_CACHE_TTL_MS = 2000L;
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("100.00");
    private static final BigDecimal MIN_POSITIVE_PRICE = new BigDecimal("0.01");

    private static final Map<String, BigDecimal> MARKET_PRICES = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_PRICE_REFRESH_MS = new ConcurrentHashMap<>();

    @Value("${market.api.insecure-trust-all:false}")
    private boolean insecureTrustAll;

    @Value("${market.api.binance-base-url:https://api.binance.com/api/v3}")
    private String binanceBaseUrl;

    private HttpClient httpClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initHttpClient() {
        this.httpClient = createHttpClient(insecureTrustAll);
        if (insecureTrustAll) {
            log.warn("Market API TLS verification is DISABLED (market.api.insecure-trust-all=true). Use only for local development.");
        }
    }

    static {
        MARKET_PRICES.put("BTC/USDT", new BigDecimal("42500.00"));
        MARKET_PRICES.put("ETH/USDT", new BigDecimal("2300.00"));
        MARKET_PRICES.put("BNB/USDT", new BigDecimal("350.00"));
        MARKET_PRICES.put("XRP/USDT", new BigDecimal("2.50"));
        MARKET_PRICES.put("ADA/USDT", new BigDecimal("1.05"));
        MARKET_PRICES.put("SOL/USDT", new BigDecimal("100.00"));
    }

    public BigDecimal getCurrentPrice(String tradingPair) {
        refreshPriceIfStale(tradingPair);
        return MARKET_PRICES.getOrDefault(tradingPair, DEFAULT_PRICE);
    }

    public void updatePrice(String tradingPair, BigDecimal price) {
        MARKET_PRICES.put(tradingPair, price);
        LAST_PRICE_REFRESH_MS.put(tradingPair, System.currentTimeMillis());
        log.debug("Price updated: {} = {}", tradingPair, price);
    }

    public void simulatePriceFluctuation(String tradingPair) {
        if (!refreshPriceFromExchange(tradingPair)) {
            nudgePrice(tradingPair, 3, 15);
        }
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
        int safeLimit = Math.max(20, Math.min(limit, 300));

        try {
            String symbol = toBinanceSymbol(tradingPair);
                String endpoint = binanceBaseUrl + "/klines?symbol=" + symbol
                    + "&interval=" + normalizeInterval(interval)
                    + "&limit=" + safeLimit;

            JsonNode root = performGet(endpoint);
            if (root == null || !root.isArray()) {
                return buildFallbackCandles(tradingPair, interval, safeLimit);
            }

            List<CandlestickDto> candlesticks = new ArrayList<>();
            for (JsonNode item : root) {
                if (!item.isArray() || item.size() < 7) {
                    continue;
                }

                CandlestickDto candle = CandlestickDto.builder()
                        .tradingPair(tradingPair)
                        .openTime(item.get(0).asLong())
                        .open(new BigDecimal(item.get(1).asText()).setScale(2, RoundingMode.HALF_UP))
                        .high(new BigDecimal(item.get(2).asText()).setScale(2, RoundingMode.HALF_UP))
                        .low(new BigDecimal(item.get(3).asText()).setScale(2, RoundingMode.HALF_UP))
                        .close(new BigDecimal(item.get(4).asText()).setScale(2, RoundingMode.HALF_UP))
                        .volume(new BigDecimal(item.get(5).asText()).setScale(2, RoundingMode.HALF_UP))
                        .closeTime(item.get(6).asLong())
                        .interval(interval)
                        .build();
                candlesticks.add(candle);
            }

            if (!candlesticks.isEmpty()) {
                updatePrice(tradingPair, candlesticks.get(candlesticks.size() - 1).getClose());
                return candlesticks;
            }
        } catch (Exception ex) {
            log.warn("Falling back to generated candles for {}: {}", tradingPair, ex.getMessage());
        }

        return buildFallbackCandles(tradingPair, interval, safeLimit);
    }

    public PriceTickDto getPriceTick(String tradingPair) {
        try {
            String symbol = toBinanceSymbol(tradingPair);
            String endpoint = binanceBaseUrl + "/ticker/bookTicker?symbol=" + symbol;
            JsonNode root = performGet(endpoint);

            if (root != null && root.hasNonNull("bidPrice") && root.hasNonNull("askPrice")) {
                BigDecimal bid = new BigDecimal(root.get("bidPrice").asText()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal ask = new BigDecimal(root.get("askPrice").asText()).setScale(2, RoundingMode.HALF_UP);
                BigDecimal qty = root.hasNonNull("bidQty")
                        ? new BigDecimal(root.get("bidQty").asText()).setScale(6, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal price = bid.add(ask).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);

                updatePrice(tradingPair, price);

                return PriceTickDto.builder()
                        .tradingPair(tradingPair)
                        .price(price)
                        .bidPrice(bid)
                        .askPrice(ask)
                        .quantity(qty)
                        .timestamp(System.currentTimeMillis())
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Live price fetch failed for {}: {}", tradingPair, ex.getMessage());
        }

        try {
            BigDecimal livePrice = fetchPriceFromCoinbase(tradingPair);
            if (livePrice != null) {
                updatePrice(tradingPair, livePrice);
                BigDecimal spread = livePrice.multiply(new BigDecimal("0.0008")).setScale(2, RoundingMode.HALF_UP);
                return PriceTickDto.builder()
                        .tradingPair(tradingPair)
                        .price(livePrice)
                        .bidPrice(livePrice.subtract(spread))
                        .askPrice(livePrice.add(spread))
                        .quantity(BigDecimal.ZERO)
                        .timestamp(System.currentTimeMillis())
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Coinbase live fetch failed for {}: {}", tradingPair, ex.getMessage());
        }

        BigDecimal price = nudgePrice(tradingPair, 2, 10);
        BigDecimal spread = price.multiply(new BigDecimal("0.001")).setScale(2, RoundingMode.HALF_UP);
        return PriceTickDto.builder()
                .tradingPair(tradingPair)
                .price(price)
                .bidPrice(price.subtract(spread))
                .askPrice(price.add(spread))
                .quantity(BigDecimal.ZERO)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    private void refreshPriceIfStale(String tradingPair) {
        long now = System.currentTimeMillis();
        Long last = LAST_PRICE_REFRESH_MS.get(tradingPair);
        if (last == null || now - last > PRICE_CACHE_TTL_MS) {
            if (!refreshPriceFromExchange(tradingPair)) {
                nudgePrice(tradingPair, 2, 8);
            }
        }
    }

    private boolean refreshPriceFromExchange(String tradingPair) {
        try {
            String symbol = toBinanceSymbol(tradingPair);
            String endpoint = binanceBaseUrl + "/ticker/price?symbol=" + symbol;
            JsonNode root = performGet(endpoint);
            if (root != null && root.hasNonNull("price")) {
                BigDecimal price = new BigDecimal(root.get("price").asText()).setScale(2, RoundingMode.HALF_UP);
                updatePrice(tradingPair, price);
                return true;
            }
        } catch (Exception ex) {
            log.warn("Live refresh failed for {}: {}", tradingPair, ex.getMessage());
        }

        try {
            BigDecimal price = fetchPriceFromCoinbase(tradingPair);
            if (price != null) {
                updatePrice(tradingPair, price);
                return true;
            }
        } catch (Exception ex) {
            log.warn("Coinbase refresh failed for {}: {}", tradingPair, ex.getMessage());
        }

        return false;
    }

    private JsonNode performGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(4))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            if (response.statusCode() == 403 || response.statusCode() == 451) {
                log.debug("External API returned status {} for {}", response.statusCode(), url);
            } else {
                log.warn("External API returned status {} for {}", response.statusCode(), url);
            }
            return null;
        }
        return objectMapper.readTree(response.body());
    }

    private HttpClient createHttpClient(boolean trustAll) {
        try {
            if (!trustAll) {
                return HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build();
            }

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("");

            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .sslContext(sslContext)
                    .sslParameters(sslParameters)
                    .build();
        } catch (Exception ex) {
            log.warn("Failed to create custom HttpClient; falling back to default TLS verification: {}", ex.getMessage());
            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
        }
    }

    private String toBinanceSymbol(String tradingPair) {
        return normalizePair(tradingPair).replace("/", "");
    }

    private String normalizePair(String tradingPair) {
        if (tradingPair == null || tradingPair.isBlank()) {
            return "BTC/USDT";
        }
        String trimmed = tradingPair.trim().toUpperCase(Locale.ROOT);
        if (trimmed.contains("/")) {
            return trimmed;
        }
        if (trimmed.endsWith("USDT") && trimmed.length() > 4) {
            return trimmed.substring(0, trimmed.length() - 4) + "/USDT";
        }
        return trimmed;
    }

    private String normalizeInterval(String interval) {
        if (interval == null || interval.isBlank()) {
            return "1h";
        }
        return interval.toLowerCase(Locale.ROOT);
    }

    private BigDecimal fetchPriceFromCoinbase(String tradingPair) throws IOException, InterruptedException {
        String symbol = toCoinbaseSymbol(tradingPair);
        if (symbol == null) {
            return null;
        }

        String endpoint = COINBASE_BASE_URL + "/prices/" + symbol + "/spot";

        JsonNode root = performGet(endpoint);
        if (root == null || !root.has("data") || !root.get("data").has("amount")) {
            return null;
        }

        return new BigDecimal(root.get("data").get("amount").asText())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String toCoinbaseSymbol(String tradingPair) {
        String normalized = normalizePair(tradingPair);
        String base = normalized.contains("/")
                ? normalized.substring(0, normalized.indexOf('/'))
                : normalized;

        switch (base) {
            case "BTC":
                return "BTC-USD";
            case "ETH":
                return "ETH-USD";
            case "BNB":
                return "BNB-USD";
            case "XRP":
                return "XRP-USD";
            case "ADA":
                return "ADA-USD";
            case "SOL":
                return "SOL-USD";
            default:
                return null;
        }
    }

    private List<CandlestickDto> buildFallbackCandles(String tradingPair, String interval, int limit) {
        List<CandlestickDto> candlesticks = new ArrayList<>();
        BigDecimal current = MARKET_PRICES.getOrDefault(tradingPair, DEFAULT_PRICE);
        long now = System.currentTimeMillis();
        long intervalMs = getIntervalMs(interval);

        for (int i = limit - 1; i >= 0; i--) {
            long openTime = now - (i * intervalMs);
            long closeTime = openTime + intervalMs;
            BigDecimal open = current;
            BigDecimal close = applyBpsMove(open, randomSignedBps(5, 28));
            BigDecimal high = applyBpsMove(open.max(close), randomUnsignedBps(2, 10));
            BigDecimal low = applyBpsMove(open.min(close), -randomUnsignedBps(2, 10));
            BigDecimal volume = new BigDecimal(ThreadLocalRandom.current().nextDouble(25, 750)).setScale(2, RoundingMode.HALF_UP);

            candlesticks.add(CandlestickDto.builder()
                    .tradingPair(tradingPair)
                    .openTime(openTime)
                    .closeTime(closeTime)
                    .open(open)
                    .high(high.max(open).max(close))
                    .low(low.min(open).min(close))
                    .close(close)
                    .volume(volume)
                    .interval(interval)
                    .build());

            current = close;
        }

        updatePrice(tradingPair, current);

        return candlesticks;
    }

    private BigDecimal nudgePrice(String tradingPair, int minBps, int maxBps) {
        BigDecimal current = MARKET_PRICES.getOrDefault(tradingPair, DEFAULT_PRICE);
        BigDecimal nudged = applyBpsMove(current, randomSignedBps(minBps, maxBps));
        updatePrice(tradingPair, nudged);
        return nudged;
    }

    private BigDecimal applyBpsMove(BigDecimal price, int bps) {
        BigDecimal factor = BigDecimal.ONE
                .add(new BigDecimal(bps).divide(new BigDecimal("10000"), 8, RoundingMode.HALF_UP));
        BigDecimal moved = price.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        if (moved.compareTo(MIN_POSITIVE_PRICE) < 0) {
            return MIN_POSITIVE_PRICE;
        }
        return moved;
    }

    private int randomSignedBps(int minAbsBps, int maxAbsBps) {
        int magnitude = randomUnsignedBps(minAbsBps, maxAbsBps);
        return ThreadLocalRandom.current().nextBoolean() ? magnitude : -magnitude;
    }

    private int randomUnsignedBps(int minBps, int maxBps) {
        return ThreadLocalRandom.current().nextInt(minBps, maxBps + 1);
    }

    private long getIntervalMs(String interval) {
        switch (normalizeInterval(interval)) {
            case "1m":
                return 60 * 1000L;
            case "5m":
                return 5 * 60 * 1000L;
            case "15m":
                return 15 * 60 * 1000L;
            case "30m":
                return 30 * 60 * 1000L;
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
}
