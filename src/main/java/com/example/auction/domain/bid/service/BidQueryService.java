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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// 입찰 조회, 결과 조회, 내입찰조회
@Service
@RequiredArgsConstructor
@Slf4j
public class BidQueryService {

    private final BidRepository bidRepository;
    // 내 입찰 조회
    public List<BidListResponse> getMyBids(AuthUser authUser, Long auctionId) {

    }

    // 특정 경매의 입찰조회
    public PageResponse<BidListResponse> getBids(AuthUser authUser, Long auctionId, Pageable pageable) {
        Auction auction = getAuction(auctionId);

        return bidRepository.findAllByAuctionIdOrderByCreatedAtDesc(auctionId)
                .stream()
                .map(bid -> {
                        return BidListResponse.builder()
                                .bidId(bid.getId())
                                .auctionId(bid.getAuctionId())
                                .price(null)
                                .status(bid.getStatus())
                                .createdAt(bid.getCreatedAt())
                                .build();

                    return BidListResponse.from(bid);
                })
                .collect(Collectors.toList());
    }

    // 입찰 결과 조회(1건)
    public BidResponse getWinnerBid(AuthUser authUser, Long auctionId) {

        // todo: 경매 존재 여부 및 상태 확인
        /*
           Auction auction = auctionRepository.findById(auctionId)
                 .orElseThrow(() -> new ServiceErrorException(BidErrorEnum.AUCTION_NOT_FOUND));
         if (!auction.getStatus().equals(AuctionStatus.DONE)) {
             throw new ServiceErrorException(BidErrorEnum.AUCTION_RESULT_NOT_FOUND);

         */

        // 최저가 입찰 1건 조회
        Bid winnerBid = bidRepository.findWinnerBidByAuctionId(auctionId)
                .orElseThrow(() -> new ServiceErrorException(BidErrorEnum.AUCTION_RESULT_NOT_FOUND));

        return BidResponse.of(winnerBid);

    }
}
