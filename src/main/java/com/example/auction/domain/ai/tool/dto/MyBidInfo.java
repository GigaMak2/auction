package com.example.auction.domain.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyBidInfo(
        Long auctionId,
        String itemName,
        String auctionStatus,
        LocalDateTime auctionEndedAt,
        BigDecimal myLowestBidPrice,
        BigDecimal currentLowestPrice,
        boolean isMyBidLowest
) {}