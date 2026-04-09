package com.example.auction.domain.bid.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// 입찰 조회, 결과 조회, 내입찰조회
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BidQueryService {

    private final BidRepository bidRepository;
    // 내 입찰 조회
    public PageResponse<BidListResponse> getMyBids(AuthUser authUser, Pageable pageable) {

        Long userId = authUser.getUserId();

        // 내 입찰 목록 조회 (페이징)
        Page<BidListResponse> myBidPage = bidRepository.findAllByUserId(userId, pageable)
                .map(BidListResponse::of);

        log.info("[내 입찰 조회] userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        return PageResponse.create(myBidPage);
    }

    // 특정 경매의 입찰조회
    public PageResponse<BidListResponse> getBids(AuthUser authUser, Long auctionId, Pageable pageable) {
        // todo: 경매 존재 여부 및 상태 확인

        Page<BidListResponse> bidPage = bidRepository.findAllByAuctionId(auctionId, pageable)
                .map(BidListResponse::of);

        return PageResponse.create(bidPage);
    }

    // 입찰 결과 조회(1건)
    public BidResponse getWinnerBid(AuthUser authUser, Long auctionId) {

        // todo: 경매 존재 여부 및 상태 확인

        // 최저가 입찰 1건 조회
        Bid winnerBid = bidRepository.findWinnerBidByAuctionId(auctionId)
                .orElseThrow(() -> new ServiceErrorException(BidErrorEnum.AUCTION_RESULT_NOT_FOUND));

        return BidResponse.of(winnerBid);

    }
}
