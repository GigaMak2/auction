package com.example.auction.domain.bid.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.bid.dto.response.BidCachedResponse;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.exception.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidQueryService {

    private final BidCacheService bidCacheService;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionResultRepository resultRepository;

    public PageResponse<BidListResponse> getMyBids(CustomUserDetails userDetails, Pageable pageable) {

        Long userId = userDetails.getUserId();

        Page<BidListResponse> myBidPage = bidRepository.findAllByUserId(userId, pageable)
                .map(BidListResponse::from);

        return PageResponse.create(myBidPage);
    }

    public PageResponse<BidListResponse> getBids(Long auctionId, Pageable pageable) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND);
        }

        Page<BidListResponse> bidPage = bidRepository.findAllByAuctionId(auctionId, pageable)
                .map(BidListResponse::from);

        return PageResponse.create(bidPage);
    }

    public BidResponse getWinnerBid(Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.DONE) {
            throw new ServiceErrorException(BidErrorEnum.AUCTION_RESULT_NOT_FOUND);
        }

        AuctionResult auctionResult = resultRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new ServiceErrorException(BidErrorEnum.AUCTION_RESULT_NOT_FOUND));

        return bidRepository.findById(auctionResult.getBidId())
                .map(BidResponse::of)
                .orElseThrow(() -> new ServiceErrorException(BidErrorEnum.BID_NOT_FOUND));

    }

    public BidResponse getCurrentMinBid(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND);
        }

        BidCachedResponse cached = bidCacheService.getCurrentMinPrice(auctionId);
        if (cached == null) {
            throw new ServiceErrorException(BidErrorEnum.BID_NOT_FOUND);
        }

        return BidResponse.fromCached(cached);
    }
}
