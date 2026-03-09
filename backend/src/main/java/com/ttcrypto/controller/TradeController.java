package com.ttcrypto.controller;

import com.ttcrypto.dto.trade.TradeDto;
import com.ttcrypto.entity.User;
import com.ttcrypto.exception.ResourceNotFoundException;
import com.ttcrypto.repository.UserRepository;
import com.ttcrypto.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/trades")
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/pair/{tradingPair}")
    public ResponseEntity<Page<TradeDto>> getTradingPairTrades(
            @PathVariable String tradingPair,
            Pageable pageable) {
        log.info("Fetching trades for pair: {}", tradingPair);
        Page<TradeDto> trades = tradeService.getTradingPairTrades(tradingPair, pageable);
        return ResponseEntity.ok(trades);
    }

    @GetMapping("/pair/{tradingPair}/recent")
    public ResponseEntity<List<TradeDto>> getRecentTrades(@PathVariable String tradingPair) {
        log.info("Fetching recent trades for pair: {}", tradingPair);
        List<TradeDto> trades = tradeService.getRecentTrades(tradingPair);
        return ResponseEntity.ok(trades);
    }

    @GetMapping
    public ResponseEntity<Page<TradeDto>> getUserTrades(
            Authentication authentication,
            Pageable pageable) {
        log.info("Fetching trades for user: {}", authentication.getName());
        Long userId = getUserIdFromAuthentication(authentication);
        Page<TradeDto> trades = tradeService.getUserTrades(userId, pageable);
        return ResponseEntity.ok(trades);
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
