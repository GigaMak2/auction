package com.example.auction.domain.bid.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.enums.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 입찰 생성
@Service
@RequiredArgsConstructor
@Slf4j
public class BidCommandService {

    private final BidRepository bidRepository;

    // 입찰 생성
    @Transactional
    public BidResponse placeBid(AuthUser authUser, Long auctionId, BidRequest request) {

        Long userId = authUser.getUserId();
        Long bidPrice = request.getPrice();

        // 경매 존재 여부 확인
        // 경매 상태 검증
        // 본인 경매 입찰 금지(경매의 유저아이디 == 입찰의 유저아이디)
        // 경매의 최대가격 초과 방지
        Long currentMinPrice = bidRepository.findMinPriceByAuctionId(auctionId).orElse(null);

        if (currentMinPrice != null && bidPrice >= currentMinPrice) {
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

        // 카프카 이벤트 발행

        return BidResponse.of(savedBid);
    }


}
