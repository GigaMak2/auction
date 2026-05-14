package com.example.auction.domain.ai.tool;

import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.example.auction.domain.ai.tool.dto.CategoryAuctionStats;
import com.example.auction.domain.ai.tool.dto.MyAuctionInfo;
import com.example.auction.domain.ai.tool.dto.MyBidInfo;
import com.example.auction.domain.ai.tool.dto.SellerReviewSummary;

import java.util.List;

public interface AiToolRepository {

    List<AuctionBidInfo> findBidsByAuctionId(Long auctionId);

    List<AuctionResultInfo> findRecentAuctionResultsByItemName(String itemName);

    long countSellerSales(Long sellerId);

    SellerReviewSummary findSellerReviewSummary(Long sellerId);

    List<MyAuctionInfo> findMyAuctions(Long userId);

    List<MyBidInfo> findMyBids(Long userId);

    List<CategoryAuctionStats> findAuctionStatsByCategory(String categoryName);
}
