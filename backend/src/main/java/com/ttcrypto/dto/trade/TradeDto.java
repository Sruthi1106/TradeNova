package com.ttcrypto.dto.trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeDto {
    private Long id;
    private String tradingPair;
    private Long buyerId;
    private Long sellerId;
    private String buyerName;
    private String sellerName;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    private LocalDateTime createdAt;
}
