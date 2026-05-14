package com.example.auction.domain.ai.tool.dto;

import java.util.List;

public record SellerStatsInfo(
        Long sellerId,
        long totalSales,
        Double avgScore,    // 리뷰 없으면 null
        List<String> recentReviews  // 최대 5개
) {}