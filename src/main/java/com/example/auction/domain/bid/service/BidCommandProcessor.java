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
import com.example.auction.domain.notification.dto.NotificationMessage;
import com.example.auction.domain.notification.enums.NotificationType;
import com.example.auction.domain.notification.publisher.NotificationMessagePublisher;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 입찰 생성- @Transactional
@Service
@RequiredArgsConstructor
@Slf4j
public class BidCommandProcessor {


    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final NotificationMessagePublisher notificationMessagePublisher;

    // requires_new를 붙여야 메서드가 끝날 때 커밋이 확정되어서 커밋 -> 락해제 순서가 보장됨
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BidResponse placeBid(CustomUserDetails userDetails, Long auctionId, BidRequest request) {

        Long userId = userDetails.getUserId();
        BigDecimal bidPrice = request.getPrice();

        // 유저 존재 및 삭제되지 않았는지 확인
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        // 경매 존재 여부 확인
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        // 취소된 경매 입찰 불가
        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_INVALID_STATUS);
        }

        // 경매 시간 검증(경매 시작 시간 <= 입찰 발생 시간 <= 경매 종료 시간)
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartedAt()) || now.isAfter(auction.getEndedAt())) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_INVALID_STATUS);
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
        Bid currentMin = bidRepository.findFirstByAuctionIdOrderByPriceAsc(auctionId).orElse(null);
        BigDecimal currentMinPrice = currentMin != null ? currentMin.getPrice() : null;

        // currentMinPrice 가 null일경우 bidPrice 가격검증 스킵됨
        if (currentMinPrice != null && bidPrice.compareTo(currentMinPrice) >= 0) {
            log.warn("[BidCommandProcessor] 입찰 가격 검증 실패 — auctionId={}, userId={}, price={}, currentMin={}", auctionId, userId, bidPrice, currentMinPrice); // 현재 최저가보다 높은 입찰 시도 감지용
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

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 새 입찰 발생 알림
                notificationMessagePublisher.publish(new NotificationMessage(
                        NotificationType.NEW_BID, auction.getUserId(), auctionId, auction.getItemName()
                ));

                // 최저가 갱신 알림
                if (currentMinPrice != null) {
                    notificationMessagePublisher.publish(new NotificationMessage(
                            NotificationType.LOWEST_BID_UPDATED, currentMin.getUserId(), auctionId, auction.getItemName()
                    ));
                }
            }
        });

        return BidResponse.of(savedBid);
    }

}
