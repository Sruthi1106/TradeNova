package com.ttcrypto.controller;

import com.ttcrypto.dto.market.CandlestickDto;
import com.ttcrypto.dto.market.OrderBookDto;
import com.ttcrypto.dto.market.PriceTickDto;
import com.ttcrypto.service.MarketDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/market")
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class MarketController {

    @Autowired
    private MarketDataService marketDataService;

    @GetMapping("/price/{tradingPair}")
    public ResponseEntity<PriceTickDto> getPriceTick(@PathVariable String tradingPair) {
        log.info("Fetching price for: {}", tradingPair);
        PriceTickDto priceTick = marketDataService.getPriceTick(tradingPair);
        return ResponseEntity.ok(priceTick);
    }

    @GetMapping("/price")
    public ResponseEntity<PriceTickDto> getPriceTickByQuery(@RequestParam String tradingPair) {
        log.info("Fetching price for: {}", tradingPair);
        PriceTickDto priceTick = marketDataService.getPriceTick(tradingPair);
        return ResponseEntity.ok(priceTick);
    }

    @GetMapping("/orderbook/{tradingPair}")
    public ResponseEntity<OrderBookDto> getOrderBook(@PathVariable String tradingPair) {
        log.info("Fetching order book for: {}", tradingPair);
        OrderBookDto orderBook = marketDataService.getOrderBook(tradingPair);
        return ResponseEntity.ok(orderBook);
    }

    @GetMapping("/orderbook")
    public ResponseEntity<OrderBookDto> getOrderBookByQuery(@RequestParam String tradingPair) {
        log.info("Fetching order book for: {}", tradingPair);
        OrderBookDto orderBook = marketDataService.getOrderBook(tradingPair);
        return ResponseEntity.ok(orderBook);
    }

    @GetMapping("/candlesticks/{tradingPair}")
    public ResponseEntity<List<CandlestickDto>> getCandlesticks(
            @PathVariable String tradingPair,
            @RequestParam(defaultValue = "1h") String interval,
            @RequestParam(defaultValue = "100") int limit) {
        log.info("Fetching candlesticks for: {} with interval: {}", tradingPair, interval);
        List<CandlestickDto> candlesticks = marketDataService.getCandlesticks(tradingPair, interval, limit);
        return ResponseEntity.ok(candlesticks);
    }

    @GetMapping("/candlesticks")
    public ResponseEntity<List<CandlestickDto>> getCandlesticksByQuery(
            @RequestParam String tradingPair,
            @RequestParam(defaultValue = "1h") String interval,
            @RequestParam(defaultValue = "100") int limit) {
        log.info("Fetching candlesticks for: {} with interval: {}", tradingPair, interval);
        List<CandlestickDto> candlesticks = marketDataService.getCandlesticks(tradingPair, interval, limit);
        return ResponseEntity.ok(candlesticks);
    }

    @GetMapping("/supported-pairs")
    public ResponseEntity<?> getSupportedPairs() {
        log.info("Fetching supported trading pairs");
        return ResponseEntity.ok(new Object() {
            public final String[] pairs = {
                    "BTC/USDT", "ETH/USDT", "BNB/USDT", "XRP/USDT", "ADA/USDT", "SOL/USDT"
            };
        });
    }
}
