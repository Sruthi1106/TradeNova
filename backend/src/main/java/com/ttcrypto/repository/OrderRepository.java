package com.ttcrypto.repository;

import com.ttcrypto.entity.Order;
import com.ttcrypto.entity.OrderSide;
import com.ttcrypto.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.tradingPair = :tradingPair " +
           "AND o.side = com.ttcrypto.entity.OrderSide.BUY " +
           "AND o.status IN (com.ttcrypto.entity.OrderStatus.PENDING, com.ttcrypto.entity.OrderStatus.PARTIALLY_FILLED) " +
           "ORDER BY CASE WHEN o.type = com.ttcrypto.entity.OrderType.MARKET THEN 0 ELSE 1 END, o.price DESC, o.createdAt ASC")
    List<Order> findBuyOrders(@Param("tradingPair") String tradingPair);
    
    @Query("SELECT o FROM Order o WHERE o.tradingPair = :tradingPair " +
           "AND o.side = com.ttcrypto.entity.OrderSide.SELL " +
           "AND o.status IN (com.ttcrypto.entity.OrderStatus.PENDING, com.ttcrypto.entity.OrderStatus.PARTIALLY_FILLED) " +
           "ORDER BY CASE WHEN o.type = com.ttcrypto.entity.OrderType.MARKET THEN 0 ELSE 1 END, o.price ASC, o.createdAt ASC")
    List<Order> findSellOrders(@Param("tradingPair") String tradingPair);
    
    @Query("SELECT o FROM Order o WHERE o.tradingPair = :tradingPair " +
           "AND o.side = :side " +
           "AND o.status IN :statuses " +
           "ORDER BY o.price")
    List<Order> findByTradingPairAndSideAndStatusesOrderByPrice(
            @Param("tradingPair") String tradingPair,
            @Param("side") OrderSide side,
            @Param("statuses") List<OrderStatus> statuses);

    List<Order> findByTradingPairAndStatusOrderByCreatedAtDesc(String tradingPair, OrderStatus status);
    
    List<Order> findByTradingPairAndSideAndStatusOrderByPrice(String tradingPair, OrderSide side, OrderStatus status);
}
