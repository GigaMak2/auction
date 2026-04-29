package com.example.auction.domain.review.dto;

import java.time.LocalDateTime;

public record ReviewAdminListResponse(
        Long reviewId,
        Long auctionId,
        Long reviewerId,
        Long revieweeId,
        int score,
        LocalDateTime createdAt
) {}
