package com.example.auction.domain.auction.repository;

import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.enums.AuctionStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;

import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.entity.Auction;
import org.springframework.data.domain.Pageable;

public interface CustomAuctionRepository {
    Page<@NonNull Auction> findByCondition(AuctionSearchCondition condition);
    Page<@NonNull Auction> findByUserIdAndCondition(Long userId, AuctionSearchCondition condition);

    Page<AuctionAdminListResponse> findAuctionWithConditions(Pageable pageable, AuctionStatus auctionStatus, String keyword);
}
