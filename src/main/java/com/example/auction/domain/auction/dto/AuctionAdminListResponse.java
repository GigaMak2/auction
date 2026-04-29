package com.example.auction.domain.auction.dto;

import com.example.auction.domain.auction.enums.AuctionStatus;

import java.time.LocalDateTime;

public record AuctionAdminListResponse(
        Long auctionId,
        Long userId,
        String itemName,
        Long categoryId,
        AuctionStatus status,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime cancelledAt
) {}
