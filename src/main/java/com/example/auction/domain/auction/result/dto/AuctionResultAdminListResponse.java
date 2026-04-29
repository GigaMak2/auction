package com.example.auction.domain.auction.result.dto;

import com.example.auction.domain.auction.result.entity.AuctionResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuctionResultAdminListResponse(
        Long auctionResultId,
        Long auctionId,
        Long buyerId,
        Long bidId,
        Long sellerId,
        BigDecimal price,
        LocalDateTime createdAt
) {
    public static AuctionResultAdminListResponse from(AuctionResult auctionResult) {
        return new AuctionResultAdminListResponse(
                auctionResult.getId(),
                auctionResult.getAuctionId(),
                auctionResult.getBuyerId(),
                auctionResult.getBidId(),
                auctionResult.getSellerId(),
                auctionResult.getPrice(),
                auctionResult.getCreatedAt()
        );
    }
}
