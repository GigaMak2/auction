package com.example.auction.domain.review.dto.response;

import java.time.LocalDateTime;

public record ReviewListGetResponse(
        Long reviewId,
        Long auctionId,
        Long counterpartId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        String imageUrl
) {}
