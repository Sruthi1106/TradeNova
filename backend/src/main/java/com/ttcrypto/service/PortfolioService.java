package com.ttcrypto.service;

import com.ttcrypto.dto.portfolio.PortfolioDto;
import com.ttcrypto.entity.Wallet;
import com.ttcrypto.repository.UserRepository;
import com.ttcrypto.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class PortfolioService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private MarketDataService marketDataService;

    public PortfolioDto getUserPortfolio(Long userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        
        BigDecimal totalBalance = BigDecimal.ZERO;
        List<PortfolioDto.HoldingDto> holdings = new ArrayList<>();

        // Calculate holdings in USDT value
        BigDecimal totalPortfolioValue = BigDecimal.ZERO;

        for (Wallet wallet : wallets) {
            String currency = wallet.getCurrency();
            BigDecimal quantity = wallet.getBalance();

            // Get current price
            BigDecimal currentPrice;
            if ("USDT".equals(currency)) {
                currentPrice = BigDecimal.ONE;
            } else {
                String pair = currency + "/USDT";
                currentPrice = marketDataService.getCurrentPrice(pair);
            }

            BigDecimal holdingValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
            totalPortfolioValue = totalPortfolioValue.add(holdingValue);

            if (quantity.compareTo(BigDecimal.ZERO) > 0 || "USDT".equals(currency)) {
                holdings.add(PortfolioDto.HoldingDto.builder()
                        .currency(currency)
                        .quantity(quantity)
                        .currentPrice(currentPrice)
                        .totalValue(holdingValue)
                        .profitLoss(BigDecimal.ZERO) // Simplified for now
                        .profitLossPercentage(BigDecimal.ZERO) // Simplified for now
                        .build());
            }
        }

        // Calculate total USDT balance equivalent
        BigDecimal availableBalance = wallets.stream()
                .filter(w -> "USDT".equals(w.getCurrency()))
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PortfolioDto.builder()
                .totalBalance(totalPortfolioValue)
                .availableBalance(availableBalance)
                .totalProfit(BigDecimal.ZERO) // Simplified
                .totalProfitPercentage(BigDecimal.ZERO) // Simplified
                .holdings(holdings)
                .build();
    }

    public BigDecimal getPortfolioValue(Long userId) {
        return getUserPortfolio(userId).getTotalBalance();
    }
}
