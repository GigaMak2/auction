package com.example.auction.domain.ai.tool.dto;

import java.math.BigDecimal;

public record CategoryAuctionStats(
        String categoryName,
        Double avgPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long totalCount
) {}