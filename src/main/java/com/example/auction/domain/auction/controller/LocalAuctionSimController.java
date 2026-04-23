package com.example.auction.domain.auction.controller;

import com.example.auction.common.config.RedisConfig;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 로컬/개발 환경 전용 — Lambda가 하는 경매 상태 전환을 HTTP로 직접 트리거
 * prod 프로파일에서는 Bean 등록 자체가 비활성화됨
 */
@Slf4j
@RestController
@Profile("!prod")
@RequiredArgsConstructor
@RequestMapping("/lambda/auctions")
public class LocalAuctionSimController {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final StringRedisTemplate stringRedisTemplate;

    // Lambda START 액션 재현: READY → ACTIVE
    @PostMapping("/{auctionId}/start")
    @Transactional
    public ResponseEntity<String> startAuction(@PathVariable Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.READY) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_INVALID_STATUS);
        }

        auction.activate();
        log.info("[LocalSim] 경매 시작 — auctionId={}, status={}", auctionId, auction.getStatus());
        return ResponseEntity.ok("경매 시작: auctionId=" + auctionId + ", status=" + auction.getStatus());
    }

    // Lambda END 액션 재현: ACTIVE → DONE(낙찰) 또는 NO_BID(유찰) + AuctionResult 저장 + Redis publish
    @PostMapping("/{auctionId}/end")
    @Transactional
    public ResponseEntity<String> endAuction(@PathVariable Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_INVALID_STATUS);
        }

        Optional<Bid> lowestBid = bidRepository.findFirstByAuctionIdOrderByPriceAsc(auctionId);

        if (lowestBid.isPresent()) {
            Bid bid = lowestBid.get();
            auction.close();
            auctionResultRepository.save(AuctionResult.of(
                    bid.getPrice(), auctionId, auction.getUserId(), bid.getUserId(), bid.getId()
            ));
            stringRedisTemplate.convertAndSend(
                    RedisConfig.AUCTION_EVENTS_CHANNEL,
                    "{\"eventType\":\"AUCTION_ENDED\",\"auctionId\":" + auctionId + "}"
            );
            log.info("[LocalSim] 경매 낙찰 — auctionId={}, bidId={}, price={}", auctionId, bid.getId(), bid.getPrice());
            return ResponseEntity.ok("경매 낙찰: auctionId=" + auctionId + ", bidId=" + bid.getId() + ", price=" + bid.getPrice());
        }

        auction.noBid();
        log.info("[LocalSim] 경매 유찰 — auctionId={}", auctionId);
        return ResponseEntity.ok("경매 유찰: auctionId=" + auctionId);
    }
}