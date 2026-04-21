package com.example.auction.domain.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyAuctionInfo(
        Long auctionId,
        String itemName,
        String status,
        LocalDateTime endedAt,
        BigDecimal currentLowestBid  // 입찰 없으면 null
) {}