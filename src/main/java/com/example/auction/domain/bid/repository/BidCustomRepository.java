package com.example.auction.domain.bid.repository;

import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BidCustomRepository {

    // 현재 최저가입찰 전체 조회(삭제된유저 제외)
    Optional<Bid> findFirstByAuctionIdOrderByPriceAsc(Long auctionId);

    Page<BidAdminListResponse> findBidWithConditions(Pageable pageable, BidAuctionStatus status, Long auctionId, Long userId);
}
