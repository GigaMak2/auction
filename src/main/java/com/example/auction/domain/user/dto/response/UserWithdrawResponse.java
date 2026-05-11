package com.example.auction.domain.user.dto.response;

import com.example.auction.domain.user.enums.UserRole;

import java.time.LocalDateTime;

public record UserWithdrawResponse(
        Long userId,
        String email,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {}
