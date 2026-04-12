package com.example.auction.domain.review.dto;

import java.time.LocalDateTime;

public record ReviewListByAuctionGetResponse(
        Long reviewId,
        Long reviewerId,
        Long revieweeId,
        int score,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
