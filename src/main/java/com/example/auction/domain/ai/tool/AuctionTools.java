package com.example.auction.domain.ai.tool;

import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.example.auction.domain.ai.tool.dto.SellerStatsInfo;
import com.example.auction.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

// LLM이 호출할 수 있는 AI Tool 모음 — 경매 데이터 조회 기능 3가지 제공
@Component
@RequiredArgsConstructor
public class AuctionTools {

    private final AiToolRepository aiToolRepository;
    private final ReviewRepository reviewRepository; // 평균 평점 조회 (이미 구현된 JPQL 활용)

    // 특정 경매의 입찰 목록과 최저가를 조회 — 경쟁 입찰 분석에 활용
    @Tool(description = "특정 경매 ID로 입찰 목록을 조회합니다. 입찰가 오름차순으로 정렬되어 최저가를 확인할 수 있습니다.")
    public List<AuctionBidInfo> getBidsByAuctionId(Long auctionId) {
        return aiToolRepository.findBidsByAuctionId(auctionId);
    }

    // 상품명으로 최근 낙찰 이력을 조회 — 시세 파악에 활용
    @Tool(description = "상품명으로 최근 낙찰 이력을 조회합니다. 유사 상품의 시세 파악에 활용됩니다.")
    public List<AuctionResultInfo> getRecentAuctionResults(String itemName) {
        return aiToolRepository.findRecentAuctionResultsByItemName(itemName);
    }

    // 판매자의 낙찰 횟수, 평균 평점, 최근 후기를 종합 조회 — 판매자 신뢰도 분석에 활용
    @Tool(description = "판매자 ID로 총 낙찰 횟수, 평균 평점, 최근 후기를 조회합니다. 판매자 신뢰도 분석에 활용됩니다.")
    public SellerStatsInfo getSellerStats(Long sellerId) {
        long totalSales = aiToolRepository.countSellerSales(sellerId);
        Double avgScore = reviewRepository.findAvgScoreByRevieweeId(sellerId); // 리뷰 없으면 null
        List<String> recentReviews = aiToolRepository.findRecentReviewTextsBySellerId(sellerId);

        return new SellerStatsInfo(
                sellerId,
                totalSales,
                avgScore != null ? avgScore : 0.0,  // 리뷰 없는 판매자는 0.0 처리
                recentReviews
        );
    }
}