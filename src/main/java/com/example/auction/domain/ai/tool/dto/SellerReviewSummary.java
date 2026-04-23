package com.example.auction.domain.ai.tool.dto;

import java.util.List;

public record SellerReviewSummary(
        Double avgScore,           // 전체 평균 평점 (리뷰 없으면 null)
        List<String> recentTexts   // 최근 후기 텍스트 (최대 5건)
) {}