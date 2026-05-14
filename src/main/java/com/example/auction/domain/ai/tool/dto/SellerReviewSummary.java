package com.example.auction.domain.ai.tool.dto;

import java.util.List;

public record SellerReviewSummary(
        Double avgScore,
        List<String> recentTexts
) {}