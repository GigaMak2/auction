package com.example.auction.domain.bid.dto.response;

import com.example.auction.domain.bid.enums.BidAuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidAdminListResponse(
        Long bidId,
        Long auctionId,
        Long userId,
        BigDecimal price,
        BidAuctionStatus status,
        LocalDateTime createdAt
) {}
