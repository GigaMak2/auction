package com.example.auction.domain.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuctionResultInfo(
        String itemName,
        BigDecimal price,
        LocalDateTime endedAt
) {}