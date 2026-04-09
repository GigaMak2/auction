package com.example.auction.domain.user.dto;

import com.example.auction.domain.user.enums.UserRole;

import java.time.LocalDateTime;

public record UserSignupResponse(
        Long userId,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {}
