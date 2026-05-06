package com.example.auction.domain.auction.search.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.search.document.AuctionDocument;

public record AuctionSearchResult (
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
    public static AuctionSearchResult from(Auction auction) {
        return new AuctionSearchResult(
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

    public static AuctionSearchResult from(AuctionDocument auctionDoc) {
        return new AuctionSearchResult(
            auctionDoc.getId(),
            auctionDoc.getUserId(),
            auctionDoc.getDescription(),
            auctionDoc.getMaxPrice(),
            auctionDoc.getItemName(),
            auctionDoc.getStatus(),
            auctionDoc.getStartedAt(),
            auctionDoc.getEndedAt(),
            auctionDoc.getCancelledAt(),
            auctionDoc.getCategoryId(),
            auctionDoc.getCreatedAt()
        );
    }
}
