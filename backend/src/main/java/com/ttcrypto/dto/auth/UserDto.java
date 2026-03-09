package com.ttcrypto.dto.auth;

import com.ttcrypto.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private UserRole role;
    private Boolean isActive;
}
