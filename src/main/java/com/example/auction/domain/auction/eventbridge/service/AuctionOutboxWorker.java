package com.example.auction.domain.auction.eventbridge.service;

import com.example.auction.domain.auction.eventbridge.entity.AuctionScheduleOutbox;
import com.example.auction.domain.auction.eventbridge.repository.AuctionScheduleOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionOutboxWorker {

    private final AuctionScheduleOutboxRepository outboxRepository;
    private final AuctionEventBridgeService eventBridgeService;

    private static final int MAX_ATTEMPTS = 3;

    @Scheduled(fixedDelay = 30000)
    @SchedulerLock(name = "outboxWorker", lockAtMostFor = "PT25S", lockAtLeastFor = "PT5S")
    @Transactional
    public void process() {
        List<AuctionScheduleOutbox> targets = outboxRepository.findByStatusInAndAttemptCountLessThan(
                        List.of("PENDING", "FAILED"), MAX_ATTEMPTS);

        for (AuctionScheduleOutbox outbox : targets) {
            try {
                if ("START".equals(outbox.getEventType())) {
                    eventBridgeService.registerStartSchedule(
                            outbox.getAuctionId(), outbox.getScheduledAt());
                } else {
                    eventBridgeService.registerEndSchedule(
                            outbox.getAuctionId(), outbox.getScheduledAt());
                }
                outbox.markPublished();
            } catch (Exception e) {
                outbox.incrementAttempt();
                if (outbox.getAttemptCount() >= MAX_ATTEMPTS) {
                    outbox.markFailed();
                    log.error("[Outbox] 스케줄 등록 최종 실패: auctionId={}, type={}",
                            outbox.getAuctionId(), outbox.getEventType(), e);
                } else {
                    log.warn("[Outbox] 스케줄 등록 재시도 {}/{}회: auctionId={}",
                            outbox.getAttemptCount(), MAX_ATTEMPTS, outbox.getAuctionId());
                }
            }
        }
    }
}
