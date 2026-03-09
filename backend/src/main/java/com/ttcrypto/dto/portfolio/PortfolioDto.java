package com.ttcrypto.dto.portfolio;

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
public class PortfolioDto {
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private BigDecimal totalProfit;
    private BigDecimal totalProfitPercentage;
    private List<HoldingDto> holdings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HoldingDto {
        private String currency;
        private BigDecimal quantity;
        private BigDecimal currentPrice;
        private BigDecimal totalValue;
        private BigDecimal profitLoss;
        private BigDecimal profitLossPercentage;
    }
}
