package com.example.auction.domain.auction.service;

import com.example.auction.domain.bid.enums.BidAuctionStatus;
import org.springframework.stereotype.Service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lambda에서 호출하는 내부 서비스
 * AuctionScheduler 대체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionLambdaService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionResultRepository resultRepository;

    // 경매 시작 (READY -> ACTIVE)
    @Transactional
    public void startAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        // todo: 캐시의 옥션 상태 확인 필요
        auction.activate();
        log.info("[경매 시작] auctionId={}", auctionId);

        // todo: 카프카 이벤트 발행
    }

    // 경매 종료 (ACTIVE -> DONE/NO_BID)
    @Transactional
    public void endAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        Bid winnerBid = bidRepository.findFirstByAuctionIdOrderByPriceAsc(auctionId).orElse(null);

        // todo: 캐시의 옥션 상태 확인 필요
        // 낙찰시 경매결과 생성 및 저장
        if (winnerBid != null) {
            auction.close();
            AuctionResult auctionResult = AuctionResult.of(
                    winnerBid.getPrice(),
                    auctionId,
                    auction.getUserId(),    // 구매자 (경매 생성자)
                    winnerBid.getUserId(),  // 판매자 (낙찰 입찰자)
                    winnerBid.getId()
            );
            resultRepository.save(auctionResult);
            log.info("[경매 낙찰] auctionId={}, winnerId={}", auctionId, winnerBid.getUserId());
        } else {
            // 유찰
            auction.noBid();
            log.info("[경매 유찰] auctionId={}", auctionId);
        }

        // 해당 경매의 모든 입찰 상태를 CLOSED로 변경
        List<Bid> bids = bidRepository.findAllByAuctionId(auctionId);
        bids.forEach(bid -> bid.updateStatus(BidAuctionStatus.CLOSED));

        // todo: 카프카 이벤트 발행
    }
}
