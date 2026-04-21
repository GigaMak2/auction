package com.example.auction.domain.ai.tool;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.ai.exception.AiErrorEnum;
import com.example.auction.domain.ai.exception.ToolEmptyResultException;
import com.example.auction.domain.ai.service.AuctionEmbeddingService;
import com.example.auction.domain.ai.service.ReviewEmbeddingService;
import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.example.auction.domain.ai.tool.dto.CategoryAuctionStats;
import com.example.auction.domain.ai.tool.dto.MyAuctionInfo;
import com.example.auction.domain.ai.tool.dto.MyBidInfo;
import com.example.auction.domain.ai.tool.dto.SellerStatsInfo;
import com.example.auction.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionTools {

    private final AiToolRepository aiToolRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewEmbeddingService reviewEmbeddingService;
    private final AuctionEmbeddingService auctionEmbeddingService;

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
            log.warn("[Tool] getRecentAuctionResults — no data, itemName length={}", itemName.length());
            throw new ToolEmptyResultException("해당 상품의 낙찰 이력이 없습니다. 시세나 낙찰가를 추측하지 마세요.");
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
        if (results.isEmpty()) {
            throw new ToolEmptyResultException("판매자(ID: " + sellerId + ")의 관련 후기가 없습니다. 데이터를 추측하지 마세요.");
        }
        return results;
    }

    // 내가 등록한 경매 목록 + 현재 최저 입찰가 조회
    @Tool(description = "사용자가 직접 등록한 경매 목록을 조회할 때 사용합니다. 각 경매의 현재 상태(READY/ACTIVE/DONE 등), 마감 시각, 현재 최저 입찰가를 확인할 수 있습니다. '내 경매 어때?', '내가 올린 경매 현황 알려줘' 같은 질문에 호출하세요.")
    public List<MyAuctionInfo> getMyAuctions(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        log.info("[Tool] getMyAuctions called — userId={}", userId);
        List<MyAuctionInfo> results = aiToolRepository.findMyAuctions(userId);
        if (results.isEmpty()) {
            throw new ToolEmptyResultException("등록한 경매가 없습니다. 데이터를 추측하지 마세요.");
        }
        return results;
    }

    // 내가 입찰한 경매 현황 + 현재 최저가 비교 조회
    @Tool(description = "사용자가 입찰한 경매 현황을 조회할 때 사용합니다. 내 입찰가와 현재 최저가를 비교해 현재 1위인지 확인할 수 있습니다. '내 입찰 현황 어때?', '내가 이기고 있어?', '내 입찰 목록 보여줘' 같은 질문에 호출하세요.")
    public List<MyBidInfo> getMyBids(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        log.info("[Tool] getMyBids called — userId={}", userId);
        List<MyBidInfo> results = aiToolRepository.findMyBids(userId);
        if (results.isEmpty()) {
            throw new ToolEmptyResultException("입찰한 경매가 없습니다. 데이터를 추측하지 마세요.");
        }
        return results;
    }

    // 카테고리명으로 낙찰 통계 조회 — 카테고리별 시세 분석에 활용
    @Tool(description = "특정 카테고리의 낙찰 통계(평균·최저·최고가, 건수)를 조회할 때 사용합니다. 카테고리 단위 시세 분석이나 비교가 필요할 때 호출하세요. '전자기기 평균 낙찰가 어때?', '의류 카테고리 시세 알려줘' 같은 질문에 호출하세요. categoryName은 한국어 카테고리명으로 전달하세요.")
    public List<CategoryAuctionStats> getAuctionStatsByCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        }
        log.info("[Tool] getAuctionStatsByCategory called — categoryName={}", categoryName);
        List<CategoryAuctionStats> results = aiToolRepository.findAuctionStatsByCategory(categoryName.trim());
        if (results.isEmpty()) {
            throw new ToolEmptyResultException("해당 카테고리의 낙찰 이력이 없습니다. 데이터를 추측하지 마세요.");
        }
        return results;
    }

    // 낙찰 경매의 상품명+설명 텍스트를 의미 기반으로 검색 — 상품 상태·스펙 분석에 활용
    @Tool(description = "실제 낙찰된 경매의 상품 설명을 의미 기반으로 검색할 때 사용합니다. '노트북 보통 어떤 상태로 올라와?', '아이폰 설명 어떻게 써?', '충전기 포함 상품 많아?' 같이 상품 상태나 스펙을 물을 때 호출하세요. query는 검색할 상품 상태·스펙 관련 키워드를 한국어로 전달하세요.")
    public List<String> searchAuctionDescriptions(String query) {
        if (query == null || query.isBlank()) {
            throw new ServiceErrorException(AiErrorEnum.INVALID_TOOL_PARAMETER);
        }
        log.info("[Tool] searchAuctionDescriptions called — queryLength={}", query.length());
        List<String> results = auctionEmbeddingService.search(query);
        if (results.isEmpty()) {
            throw new ToolEmptyResultException("관련 상품 설명이 없습니다. 데이터를 추측하지 마세요.");
        }
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