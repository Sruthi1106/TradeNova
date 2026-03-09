package com.ttcrypto.repository;

import com.ttcrypto.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    Page<Trade> findByTradingPairOrderByCreatedAtDesc(String tradingPair, Pageable pageable);
    
    List<Trade> findByTradingPairOrderByCreatedAtDesc(String tradingPair);
    
    Page<Trade> findByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId, Pageable pageable);
    
    List<Trade> findByBuyOrderIdOrSellOrderId(Long buyOrderId, Long sellOrderId);
}
