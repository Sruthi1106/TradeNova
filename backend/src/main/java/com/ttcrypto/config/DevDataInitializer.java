package com.ttcrypto.config;

import com.ttcrypto.entity.User;
import com.ttcrypto.entity.UserRole;
import com.ttcrypto.entity.Wallet;
import com.ttcrypto.repository.UserRepository;
import com.ttcrypto.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User demoUser = userRepository.findByEmail("demo@example.com").orElseGet(() -> User.builder()
                .email("demo@example.com")
                .username("demo")
                .firstName("Demo")
                .lastName("User")
                .totalBalance(new BigDecimal("10000.00"))
                .availableBalance(new BigDecimal("10000.00"))
                .role(UserRole.USER)
                .isActive(true)
                .build());

        demoUser.setPassword(passwordEncoder.encode("password"));
        demoUser = userRepository.save(demoUser);
        final User savedDemoUser = demoUser;
        final Long demoUserId = savedDemoUser.getId();

        List<String> currencies = Arrays.asList("USDT", "BTC", "ETH", "BNB", "XRP");
        for (String currency : currencies) {
            Wallet wallet = walletRepository.findByUserIdAndCurrency(demoUserId, currency)
                    .orElseGet(() -> Wallet.builder()
                            .user(savedDemoUser)
                            .currency(currency)
                            .balance(currency.equals("USDT") ? new BigDecimal("10000.00") : BigDecimal.ZERO)
                            .lockedBalance(BigDecimal.ZERO)
                            .build());
            walletRepository.save(wallet);
        }

        log.info("Dev demo user initialized: demo@example.com / password");
    }
}
