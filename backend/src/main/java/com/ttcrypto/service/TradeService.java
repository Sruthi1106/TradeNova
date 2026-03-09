package com.ttcrypto.service;

import com.ttcrypto.dto.trade.TradeDto;
import com.ttcrypto.entity.Trade;
import com.ttcrypto.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class TradeService {

    @Autowired
    private TradeRepository tradeRepository;

    public Page<TradeDto> getTradingPairTrades(String tradingPair, Pageable pageable) {
        return tradeRepository.findByTradingPairOrderByCreatedAtDesc(tradingPair, pageable)
                .map(this::mapTradeToDto);
    }

    public List<TradeDto> getRecentTrades(String tradingPair) {
        return tradeRepository.findByTradingPairOrderByCreatedAtDesc(tradingPair)
                .stream()
                .limit(50)
                .map(this::mapTradeToDto)
                .collect(Collectors.toList());
    }

    public Page<TradeDto> getUserTrades(Long userId, Pageable pageable) {
        return tradeRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId, pageable)
                .map(this::mapTradeToDto);
    }

    private TradeDto mapTradeToDto(Trade trade) {
        return TradeDto.builder()
                .id(trade.getId())
                .tradingPair(trade.getTradingPair())
                .buyerId(trade.getBuyer().getId())
                .sellerId(trade.getSeller().getId())
                .buyerName(trade.getBuyer().getUsername())
                .sellerName(trade.getSeller().getUsername())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .totalValue(trade.getTotalValue())
                .createdAt(trade.getCreatedAt())
                .build();
    }
}
