package com.example.auction.domain.bid.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.exceptions.BidErrorEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

// 입찰 생성 - 락 획득만 담당, 비즈니스 로직은 프로세서
// 이 클래스에 @Transactional 생기면 processor.placeBid 예외가 전파되지 않으니 주의 필요
@Service
@RequiredArgsConstructor
@Slf4j
public class BidCommandFacade {

    private final BidCommandProcessor processor;
    private final RedissonClient redissonClient;

    private static final String BID_LOCK_PREFIX = "bid:lock:";
    private static final long LOCK_WAIT_TIME = 20L;    // 락 획득 대기 시간 (초)

    // 입찰 생성 - 분산락
    public BidResponse placeBidDis(CustomUserDetails userDetails, Long auctionId, BidRequest request) {

        RLock lock = redissonClient.getLock(BID_LOCK_PREFIX + auctionId);
        boolean isLocked;

        try {
            // watchdog 사용
            isLocked = lock.tryLock(LOCK_WAIT_TIME, -1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceErrorException(BidErrorEnum.BID_LOCK_FAILED);
        }

        // 락 획득 실패할 경우 에러
        if (!isLocked) {
            log.warn("[BidCommandFacade] 락 획득 실패 — auctionId={}, userId={}, price={}", auctionId, userDetails.getUserId(), request.getPrice()); // 동시 입찰 경합 또는 락 타임아웃 감지용
            throw new ServiceErrorException(BidErrorEnum.BID_LOCK_FAILED);
        }

        try {
            return processor.placeBid(userDetails, auctionId, request);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
