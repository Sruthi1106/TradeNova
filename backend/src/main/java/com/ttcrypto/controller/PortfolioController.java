package com.ttcrypto.controller;

import com.ttcrypto.dto.portfolio.DepositRequest;
import com.ttcrypto.dto.portfolio.PortfolioDto;
import com.ttcrypto.entity.User;
import com.ttcrypto.exception.ResourceNotFoundException;
import com.ttcrypto.repository.UserRepository;
import com.ttcrypto.service.PortfolioService;
import com.ttcrypto.service.WalletService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/portfolio")
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<PortfolioDto> getUserPortfolio(Authentication authentication) {
        log.info("Fetching portfolio for user: {}", authentication.getName());
        Long userId = getUserIdFromAuthentication(authentication);
        PortfolioDto portfolio = portfolioService.getUserPortfolio(userId);
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/value")
    public ResponseEntity<Object> getPortfolioValue(Authentication authentication) {
        log.info("Fetching portfolio value for user: {}", authentication.getName());
        Long userId = getUserIdFromAuthentication(authentication);
        return ResponseEntity.ok(new Object() {
            public final Object value = portfolioService.getPortfolioValue(userId);
        });
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> depositFunds(
            Authentication authentication,
            @Valid @RequestBody DepositRequest request) {
        Long userId = getUserIdFromAuthentication(authentication);
        String normalizedCurrency = normalizeCurrency(request.getCurrency());

        walletService.addBalance(userId, normalizedCurrency, request.getAmount());
        log.info("Deposited {} {} for user {}", request.getAmount(), normalizedCurrency, userId);

        return ResponseEntity.ok(Map.of(
                "message", "Deposit successful",
                "currency", normalizedCurrency,
                "amount", request.getAmount()
        ));
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if ("USDC".equals(normalized)) {
            return "USDT";
        }
        return normalized;
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }

        String identifier = authentication.getName().trim();
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
