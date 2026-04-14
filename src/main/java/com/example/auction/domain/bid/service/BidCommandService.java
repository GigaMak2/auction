package com.example.auction.domain.bid.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.exceptions.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// 입찰 생성
@Service
@RequiredArgsConstructor
@Slf4j
public class BidCommandService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;

    // 입찰 생성
    @Transactional
    public BidResponse placeBid(CustomUserDetails userDetails, Long auctionId, BidRequest request) {

        Long userId = userDetails.getUserId();
        BigDecimal bidPrice = request.getPrice();

        // 경매 존재 여부 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        // 경매 상태 검증 (ACTIVE만 입찰 가능)
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceErrorException(BidErrorEnum.AUCTION_INVALID_STATUS);
        }

        // 경매 종료 시간 검증 (스케줄러 타이밍 오차 방지)
        if (auction.getEndedAt().isBefore(java.time.LocalDateTime.now())) {
            throw new ServiceErrorException(BidErrorEnum.AUCTION_INVALID_STATUS);
        }

        // 본인 경매 입찰 금지
        if (auction.getUserId().equals(userId)) {
            throw new ServiceErrorException(BidErrorEnum.BID_FORBIDDEN_SELF_BID);
        }

        // 경매 최대 가격 초과 방지
        if (bidPrice.compareTo(auction.getMaxPrice()) > 0) {
            throw new ServiceErrorException(BidErrorEnum.BID_PRICE_EXCEEDS_MAX);
        }

        // 현재 최저가보다 낮아야 함
        BigDecimal currentMinPrice = bidRepository.findMinPriceByAuctionId(auctionId).orElse(null);

        if (currentMinPrice != null && bidPrice.compareTo(currentMinPrice) >= 0) {
            log.warn("[입찰 실패] auctionId={}, userId={}, bidPrice={}, currentMinPrice={}",
                    auctionId, userId, bidPrice, currentMinPrice);
            throw new ServiceErrorException(BidErrorEnum.BID_PRICE_NOT_LOWER);
        }

        // 입찰 생성 및 저장
        Bid bid = Bid.of(
                request.getDescription(),
                bidPrice,
                auctionId,
                userId,
                BidAuctionStatus.ACTIVE);
        Bid savedBid = bidRepository.save(bid);

        log.info("[입찰] auctionId={}, userId={}, bidPrice={}", auctionId, userId, bidPrice);

        // todo: 카프카 이벤트 발행

        return BidResponse.of(savedBid);
    }


}
