package com.example.auction.domain.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyBidInfo(
        Long auctionId,
        String itemName,
        String auctionStatus,
        LocalDateTime auctionEndedAt,
        BigDecimal myLowestBidPrice,      // 해당 경매에서 내 최저 입찰가
        BigDecimal currentLowestPrice,    // 현재 경매 최저가
        boolean isMyBidLowest             // 내가 현재 1위인지
) {}