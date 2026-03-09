package com.ttcrypto.websocket;

import com.ttcrypto.dto.market.PriceTickDto;
import com.ttcrypto.service.MarketDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
@CrossOrigin(origins = "${websocket.allowed-origins}")
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MarketDataService marketDataService;

    @MessageMapping("/subscribe")
    @SendTo("/topic/market")
    public Map<String, String> subscribe(String tradingPair) {
        log.info("User subscribed to: {}", tradingPair);
        Map<String, String> response = new HashMap<>();
        response.put("status", "subscribed");
        response.put("tradingPair", tradingPair);
        return response;
    }

    @MessageMapping("/price")
    @SendTo("/topic/price")
    public PriceTickDto getPriceUpdate(String tradingPair) {
        log.debug("Price update request for: {}", tradingPair);
        return marketDataService.getPriceTick(tradingPair);
    }

    public void broadcastPriceUpdate(String tradingPair) {
        PriceTickDto priceTick = marketDataService.getPriceTick(tradingPair);
        messagingTemplate.convertAndSend("/topic/price/" + tradingPair, priceTick);
    }

    public void broadcastOrderUpdate(String tradingPair) {
        messagingTemplate.convertAndSend("/topic/orders/" + tradingPair, 
                marketDataService.getOrderBook(tradingPair));
    }

    @Scheduled(fixedRate = 1000) // Broadcast price updates every 1 second
    public void broadcastPriceUpdates() {
        String[] tradingPairs = {"BTC/USDT", "ETH/USDT", "BNB/USDT", "XRP/USDT", "ADA/USDT"};
        for (String tradingPair : tradingPairs) {
            marketDataService.simulatePriceFluctuation(tradingPair);
            broadcastPriceUpdate(tradingPair);
        }
    }
}
