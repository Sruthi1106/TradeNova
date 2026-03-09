package com.ttcrypto.service;

import com.ttcrypto.entity.Wallet;
import com.ttcrypto.entity.User;
import com.ttcrypto.exception.InsufficientBalanceException;
import com.ttcrypto.exception.ResourceNotFoundException;
import com.ttcrypto.repository.UserRepository;
import com.ttcrypto.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@Transactional
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    public Wallet getWallet(Long userId, String currency) {
        return walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wallet not found for currency: " + currency));
    }

    public List<Wallet> getUserWallets(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    public void addBalance(Long userId, String currency, BigDecimal amount) {
        Wallet wallet = getWallet(userId, currency);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        log.info("Added {} {} to user {}", amount, currency, userId);
    }

    public void deductBalance(Long userId, String currency, BigDecimal amount) {
        Wallet wallet = getWallet(userId, currency);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + wallet.getBalance() + ", Required: " + amount);
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        log.info("Deducted {} {} from user {}", amount, currency, userId);
    }

    public void lockBalance(Long userId, String currency, BigDecimal amount) {
        Wallet wallet = getWallet(userId, currency);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance to lock. Available: " + wallet.getBalance() + ", Required: " + amount);
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        walletRepository.save(wallet);
        log.info("Locked {} {} for user {}", amount, currency, userId);
    }

    public void unlockBalance(Long userId, String currency, BigDecimal amount) {
        Wallet wallet = getWallet(userId, currency);
        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient locked balance. Available: " + wallet.getLockedBalance() + ", Required: " + amount);
        }
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        log.info("Unlocked {} {} for user {}", amount, currency, userId);
    }

    public void consumeLockedBalance(Long userId, String currency, BigDecimal amount) {
        Wallet wallet = getWallet(userId, currency);
        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient locked balance. Available: " + wallet.getLockedBalance() + ", Required: " + amount);
        }
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        walletRepository.save(wallet);
        log.info("Consumed locked {} {} for user {}", amount, currency, userId);
    }

    public void transferBetweenWallets(Long fromUserId, Long toUserId, String currency, BigDecimal amount) {
        deductBalance(fromUserId, currency, amount);
        addBalance(toUserId, currency, amount);
        log.info("Transferred {} {} from user {} to user {}", amount, currency, fromUserId, toUserId);
    }

    public BigDecimal getAvailableBalance(Long userId, String currency) {
        Wallet wallet = getWallet(userId, currency);
        return wallet.getBalance();
    }

    public BigDecimal getTotalBalance(Long userId, String currency) {
        Wallet wallet = getWallet(userId, currency);
        return wallet.getBalance().add(wallet.getLockedBalance());
    }
}
