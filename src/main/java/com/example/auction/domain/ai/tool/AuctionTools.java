package com.example.auction.domain.ai.tool;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.ai.exception.AiErrorEnum;
import com.example.auction.domain.ai.exception.ToolEmptyResultException;
import com.example.auction.domain.ai.service.ReviewEmbeddingService;
import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.example.auction.domain.ai.tool.dto.SellerStatsInfo;
import com.example.auction.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionTools {

    private final AiToolRepository aiToolRepository;
    private final ReviewRepository reviewRepository;         // 평균 평점 조회 (이미 구현된 JPQL 활용)
    private final ReviewEmbeddingService reviewEmbeddingService; // RAG 유사도 검색

    // 특정 경매의 입찰 목록과 최저가를 조회 — 경쟁 입찰 분석에 활용
    @Tool(description = "특정 경매의 현재 입찰 경쟁 현황을 파악할 때 사용합니다. 입찰가 목록, 최저가, 입찰자 수를 확인할 수 있습니다. auctionId는 숫자 ID입니다. '경매 N번 경쟁 심해?', '지금 최저 입찰가 얼마야?' 같은 질문에 호출하세요.")
    public List<AuctionBidInfo> getBidsByAuctionId(Long auctionId) {
        log.info("[Tool] getBidsByAuctionId called — auctionId={}", auctionId);
        if (auctionId == null || auctionId <= 0) {
            throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        }
        List<AuctionBidInfo> results = aiToolRepository.findBidsByAuctionId(auctionId);
        if (results.isEmpty()) {
            throw new ToolEmptyResultException("해당 경매(ID: " + auctionId + ")의 입찰 내역이 없습니다. 입찰가나 경쟁 현황을 추측하지 마세요.");
        }
        return results;
    }

    // 상품명으로 최근 낙찰 이력을 조회 — 시세 파악에 활용
    @Tool(description = "이 플랫폼의 실제 낙찰 이력과 시세를 DB에서 조회합니다. 이 플랫폼의 낙찰가는 AI 학습 데이터에 없는 실시간 거래 데이터이므로, 시세·낙찰가 관련 질문에는 반드시 이 Tool을 호출해야 합니다. 호출 없이 시세를 답변하는 것은 금지됩니다. itemName은 한국어 상품명으로 전달하세요 (예: '노트북', '아이폰 15').")
    public List<AuctionResultInfo> getRecentAuctionResults(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        }
        log.info("[Tool] getRecentAuctionResults called — itemName={}", itemName);
        List<AuctionResultInfo> results = aiToolRepository.findRecentAuctionResultsByItemName(itemName.trim());
        log.info("[Tool] getRecentAuctionResults result count={}", results.size());
        if (results.isEmpty()) {
            log.warn("[Tool] getRecentAuctionResults — no data, throwing exception for itemName={}", itemName);
            throw new ToolEmptyResultException("'" + itemName + "'의 낙찰 이력이 없습니다. 시세나 낙찰가를 추측하지 마세요.");
        }
        return results;
    }

    // 판매자 후기 의미 검색 — RAG 기반으로 질문과 관련 있는 후기 텍스트 반환
    @Tool(description = "판매자 후기 중 특정 키워드(배송, 포장, 상품 상태 등)와 관련된 후기를 의미 기반으로 검색할 때 사용합니다. '배송 빠른 편이야?', '포장 꼼꼼해?' 같이 구체적인 항목을 물을 때 호출하세요. 일반적인 신뢰도 조회는 getSellerStats를 사용하세요. query는 검색할 키워드나 질문을 한국어로 전달하세요.")
    public List<String> getSellerReviewInsights(Long sellerId, String query) {
        log.debug("[Tool] getSellerReviewInsights called — sellerId={}, queryLength={}", sellerId, query != null ? query.length() : 0);
        if (sellerId == null || sellerId <= 0) {
            throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        }
        if (query == null || query.isBlank()) {
            throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        }
        List<String> results = reviewEmbeddingService.search(sellerId, query);
        log.info("[Tool] getSellerReviewInsights result count={}", results.size());
        return results;
    }

    // 판매자의 낙찰 횟수, 평균 평점, 최근 후기를 종합 조회 — 판매자 신뢰도 분석에 활용
    @Tool(description = "판매자의 낙찰 횟수, 평균 평점, 최근 후기를 종합 조회할 때 사용합니다. '판매자 N번 믿을 수 있어?', '거래 이력 어때?' 같은 일반적인 신뢰도 질문에 호출하세요. 배송·포장 등 특정 키워드 관련 후기 분석은 getSellerReviewInsights를 사용하세요.")
    public SellerStatsInfo getSellerStats(Long sellerId) {
        log.info("[Tool] getSellerStats called — sellerId={}", sellerId);
        if (sellerId == null || sellerId <= 0) {
            throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        }
        long totalSales = aiToolRepository.countSellerSales(sellerId);
        Double avgScore = reviewRepository.findAvgScoreByRevieweeId(sellerId); // 리뷰 없으면 null
        List<String> recentReviews = aiToolRepository.findRecentReviewTextsBySellerId(sellerId);

        return new SellerStatsInfo(
                sellerId,
                totalSales,
                avgScore,
                recentReviews
        );
    }
}