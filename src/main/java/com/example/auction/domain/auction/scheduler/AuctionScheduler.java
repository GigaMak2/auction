package com.example.auction.domain.auction.scheduler;

import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    /*
     startedAt 이 지난 경매 시작 처리(READY -> ACTIVE)
     10초마다 실행
     */
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void toActiveAuctions() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> readyAuctions = auctionRepository.findAllByStatusAndStartedAtBefore(
                AuctionStatus.READY, now);

        for (Auction auction : readyAuctions) {
            auction.activate();
            log.info("[경매 시작] auctionId={}", auction.getId());
        }
    }

    // active -> done/nobid 처리
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void endAuctions() {

        LocalDateTime now = LocalDateTime.now();
        List<Auction> activeAuctions = auctionRepository.findAllByStatusAndEndedAtBefore(
                AuctionStatus.ACTIVE, now);

        for (Auction auction : activeAuctions) {
            Long auctionId = auction.getId();

            // 입찰 존재 여부 확인
            boolean hasBid = bidRepository.findMinPriceByAuctionId(auctionId).isPresent();

            if (hasBid) {
                // 낙찰
                auction.close();
                log.info("[경매 낙찰] auctionId={}", auctionId);
            } else {
                // 유찰
                auction.noBid();
                log.info("[경매 유찰] auctionId={}", auctionId);
            }
        }
    }
}
