package com.ttcrypto.dto.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderBookDto {
    private String tradingPair;
    private List<OrderBookLevel> bids;
    private List<OrderBookLevel> asks;
    private Long lastUpdateTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderBookLevel {
        private BigDecimal price;
        private BigDecimal quantity;
    }
}
