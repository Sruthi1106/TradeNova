package com.ttcrypto.service;

import com.ttcrypto.dto.auth.LoginRequest;
import com.ttcrypto.dto.auth.LoginResponse;
import com.ttcrypto.dto.auth.RegisterRequest;
import com.ttcrypto.dto.auth.UserDto;
import com.ttcrypto.entity.User;
import com.ttcrypto.entity.UserRole;
import com.ttcrypto.entity.Wallet;
import com.ttcrypto.exception.DuplicateResourceException;
import com.ttcrypto.exception.ResourceNotFoundException;
import com.ttcrypto.repository.UserRepository;
import com.ttcrypto.repository.WalletRepository;
import com.ttcrypto.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .totalBalance(new BigDecimal("10000.00"))
                .availableBalance(new BigDecimal("10000.00"))
                .role(UserRole.USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        // Initialize wallets for common cryptocurrencies
        List<String> defaultCurrencies = Arrays.asList("USDT", "BTC", "ETH", "BNB", "XRP");
        defaultCurrencies.forEach(currency -> {
            Wallet wallet = Wallet.builder()
                    .user(savedUser)
                    .currency(currency)
                    .balance(currency.equals("USDT") ? new BigDecimal("10000.00") : BigDecimal.ZERO)
                    .lockedBalance(BigDecimal.ZERO)
                    .build();
            walletRepository.save(wallet);
        });

        return mapUserToDto(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                identifier,
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userRepository.findByUsername(identifier)
            .orElseGet(() -> userRepository.findByEmail(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        log.info("User logged in successfully: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime() / 1000)
                .user(mapUserToDto(user))
                .build();
    }

    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return mapUserToDto(user);
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .totalBalance(user.getTotalBalance())
                .availableBalance(user.getAvailableBalance())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .build();
    }
}
