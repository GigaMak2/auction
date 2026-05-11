package com.example.auction.domain.review.dto.response;

import java.time.LocalDateTime;

public record ReviewGetResponse(
        Long reviewId,
        Long auctionId,
        Long reviewerId,
        Long revieweeId,
        int score,
        String description,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
