package com.example.auction.domain.ai.tool;

import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.example.auction.domain.ai.tool.dto.CategoryAuctionStats;
import com.example.auction.domain.ai.tool.dto.MyAuctionInfo;
import com.example.auction.domain.ai.tool.dto.MyBidInfo;
import com.example.auction.domain.ai.tool.dto.SellerReviewSummary;

import java.util.List;

// AI Tool 전용 QueryDSL 조회 인터페이스 (다중 도메인 데이터 조회)
public interface AiToolRepository {

    // 경매 ID로 입찰 목록 조회 (가격 오름차순)
    List<AuctionBidInfo> findBidsByAuctionId(Long auctionId);

    // 상품명으로 최근 낙찰 이력 조회 (최대 10건)
    List<AuctionResultInfo> findRecentAuctionResultsByItemName(String itemName);

    // 판매자의 총 낙찰 횟수 조회
    long countSellerSales(Long sellerId);

    // 판매자 평균 평점 + 최근 후기 텍스트를 한 번에 조회 — ReviewRepository 의존 제거
    SellerReviewSummary findSellerReviewSummary(Long sellerId);

    // 내가 등록한 경매 목록 + 현재 최저 입찰가 (최대 10건, 마감일 오름차순)
    List<MyAuctionInfo> findMyAuctions(Long userId);

    // 내가 입찰한 경매 현황 + 현재 최저가 비교 (최대 10건, 마감일 내림차순)
    List<MyBidInfo> findMyBids(Long userId);

    // 카테고리명으로 낙찰 통계 조회 (평균·최저·최고가, 건수)
    List<CategoryAuctionStats> findAuctionStatsByCategory(String categoryName);
}
