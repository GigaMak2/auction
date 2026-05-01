package com.example.auction.domain.user.dto;

import com.example.auction.domain.user.enums.UserRole;

import java.time.LocalDateTime;

public record UserListGetResponse(
        Long userId,
        String email,
        UserRole role,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {}
