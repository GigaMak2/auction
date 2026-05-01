package com.example.auction.domain.auction.search.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;

public record AuctionCreatedDocument(
    Long id,

    Long userId,

    String description,

    BigDecimal maxPrice,

    String itemName,

    AuctionStatus status,

    LocalDateTime startedAt,

    LocalDateTime endedAt,

    LocalDateTime cancelledAt,

    Long categoryId,

    LocalDateTime createdAt
) {
    public static AuctionCreatedDocument from(Auction auction){
        return new AuctionCreatedDocument(
            auction.getId(),

            auction.getUserId(),

            auction.getDescription(),

            auction.getMaxPrice(),

            auction.getItemName(),

            auction.getStatus(),

            auction.getStartedAt(),

            auction.getEndedAt(),

            auction.getCancelledAt(),

            auction.getCategoryId(),

            auction.getCreatedAt()
        );
    }
}
