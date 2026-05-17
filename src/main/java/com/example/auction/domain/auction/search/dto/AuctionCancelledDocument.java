package com.example.auction.domain.auction.search.dto;

import com.example.auction.domain.auction.entity.Auction;

import java.time.LocalDateTime;

public record AuctionCancelledDocument (
        Long auctionId,
        LocalDateTime cancelledAt
) {
    public static AuctionCancelledDocument from(
            Auction auction
    ) {
        return new AuctionCancelledDocument(
                auction.getId(),
                auction.getCancelledAt()
        );
    }
}
