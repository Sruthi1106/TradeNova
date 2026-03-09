package com.ttcrypto.repository;

import com.ttcrypto.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserIdAndCurrency(Long userId, String currency);
    List<Wallet> findByUserId(Long userId);
    boolean existsByUserIdAndCurrency(Long userId, String currency);
}
