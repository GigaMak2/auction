package com.example.auction.domain.user.dto.response;

import com.example.auction.domain.user.enums.AuthProvider;
import com.example.auction.domain.user.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserGetResponse(
        Long userId,
        String email,
        BigDecimal rating,
        UserRole role,
        AuthProvider provider,
        LocalDateTime createdAt
) {}
