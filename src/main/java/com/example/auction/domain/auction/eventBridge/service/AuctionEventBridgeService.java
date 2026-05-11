
package com.example.auction.domain.auction.eventBridge.service;

import com.example.auction.domain.auction.eventBridge.entity.AuctionCancelledEventBridge;
import com.example.auction.domain.auction.eventBridge.entity.AuctionCreatedEventBridge;
import com.example.auction.domain.auction.eventBridge.repository.AuctionScheduleOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion;
import software.amazon.awssdk.services.scheduler.model.ConflictException;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;
import software.amazon.awssdk.services.scheduler.model.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEventBridgeService {

    private final SchedulerClient schedulerClient;
    private final AuctionScheduleOutboxRepository outboxRepository;

    // 이벤트브릿지가 어떤 람다 함수를 실행할지 찾을 때 쓰는 경로. 계정번호와 실제 람다함수이름이 필요함
    @Value("${aws.eventbridge.lambda-arn}")
    private String lambdaArn;
    // 이벤트브릿지가 람다함수 실행권한이 있음을 증명할 때 사용
    @Value("${aws.eventbridge.role-arn}")
    private String roleArn;

    @Value("${aws.eventbridge.legacy-lambda-arn}")
    private String legacyLambdaArn;

    private static final DateTimeFormatter TARGET_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String newArnSuffix = "NEW";
    private static final String legacyArnSuffix = "LEGACY";


    // 트랜잭션 커밋 이후 eventBridge 스케줄 등록하여 고아 스케줄 방지(db에는 없고 aws 스케줄에만 있는 경우 방지)
    // 3회 재시도
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionCreated(AuctionCreatedEventBridge event) {
        int maxAttempts = 3;
        // start/end 따로 시도
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                registerStartSchedule(event.auctionId(), event.startedAt());
                outboxRepository.markPublished(event.auctionId(), "START");
                break;
            } catch (Exception e) {
                log.warn("[EventBridge] START 스케줄 등록 실패 {}/{}회 - auctionId={}", attempt, maxAttempts, event.auctionId(), e);
                if (attempt == maxAttempts) {
                    log.error("[EventBridge] START 스케줄 등록 최종 실패 - auctionId={}", event.auctionId(), e);
                }
            }
        }

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                registerEndSchedule(event.auctionId(), event.endedAt());
                outboxRepository.markPublished(event.auctionId(), "END");
                break;
            } catch (Exception e) {
                log.warn("[EventBridge] END 스케줄 등록 실패 {}/{}회 - auctionId={}", attempt, maxAttempts, event.auctionId(), e);
                if (attempt == maxAttempts) {
                    log.error("[EventBridge] END 스케줄 등록 최종 실패 - auctionId={}", event.auctionId(), e);
                }
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionCancelled(AuctionCancelledEventBridge event) {
        deleteSchedules(event.auctionId());
    }

    // 경매 시작 스케줄 등록(5분 전)
    public void registerStartSchedule(Long auctionId, LocalDateTime startedAt) {
        if (isPast(startedAt.minusMinutes(5))) {
            log.warn("[EventBridge] 현재보다 과거로 시작 시간 등록: auctionId={}", auctionId);
            return;
        }
        // 신규와 레거시 둘다 등록
        registerSchedule(auctionId, startedAt.minusMinutes(5), startedAt, "START", lambdaArn, newArnSuffix);
        //registerSchedule(auctionId, startedAt, startedAt,"START", legacyLambdaArn, legacyArnSuffix);
    }

    // 경매 종료 스케줄 등록(5분 전)
    public void registerEndSchedule(Long auctionId, LocalDateTime endedAt) {
        if (isPast(endedAt.minusMinutes(5))) {
            log.warn("[EventBridge] 현재보다 과거로 종료 시간 등록: auctionId={}", auctionId);
            return;
        }
        registerSchedule(auctionId, endedAt.minusMinutes(5), endedAt, "END", lambdaArn, newArnSuffix);
        //registerSchedule(auctionId, endedAt, endedAt, "END", legacyLambdaArn, legacyArnSuffix);

    }

    // KST 기준 시간이 현재 UTC보다 과거인지 확인
    private boolean isPast(LocalDateTime kstTime) {
        LocalDateTime utc = kstTime.atZone(ZoneId.of("Asia/Seoul"))
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime();
        return utc.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private void registerSchedule(
            Long auctionId, LocalDateTime minusTime, LocalDateTime realTime, String action, String targetArn, String suffix
    ) {
        String scheduleName = "auction-" + action.toLowerCase() + "-" + auctionId + "-" + suffix;
        String atExpression = toAt(minusTime);
        String input = String.format(
                    "{\"auctionId\":%d,\"action\":\"%s\",\"targetTime\":\"%s\"}",
                    auctionId, action, toUtcString(realTime)
            );


        try {
            schedulerClient.createSchedule(r -> r
                    .name(scheduleName)
                    .scheduleExpression(atExpression)
                    .scheduleExpressionTimezone("UTC")
                    .flexibleTimeWindow(w -> w.mode(FlexibleTimeWindowMode.OFF))
                    .target(t -> t
                            .arn(targetArn)
                            .roleArn(roleArn)
                            .input(input))
                    .actionAfterCompletion(ActionAfterCompletion.DELETE) // 실행 후 자동 삭제(경매 시작/종료는 1번씩이니까)
            );
        } catch (ConflictException e) {
            log.warn("[EventBridge] 기존 스케줄 존재, 업데이트: {}", scheduleName);
            schedulerClient.updateSchedule(r -> r
                    .name(scheduleName)
                    .scheduleExpression(atExpression)
                    .scheduleExpressionTimezone("UTC")
                    .flexibleTimeWindow(w -> w.mode(FlexibleTimeWindowMode.OFF))
                    .target(t -> t
                            .arn(targetArn)
                            .roleArn(roleArn)
                            .input(input))
                    .actionAfterCompletion(ActionAfterCompletion.DELETE) // 실행 후 자동 삭제(경매 시작/종료는 1번씩이니까)
            );
        }
    }

    public void deleteSchedules(Long auctionId) {
        deleteSchedule("auction-start-" + auctionId + "-NEW");
        deleteSchedule("auction-start-" + auctionId + "-LEGACY");
        deleteSchedule("auction-end-" + auctionId + "-NEW");
        deleteSchedule("auction-end-" + auctionId + "-LEGACY");
    }

    private void deleteSchedule(String scheduleName) {
        int maxAttempts = 3;
        for(int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                schedulerClient.deleteSchedule(r -> r.name(scheduleName));
                log.info("[EventBridge] 스케줄 삭제: {}", scheduleName);
                return;
            } catch (ResourceNotFoundException e) {
                log.warn("[EventBridge] 스케줄 없음 (이미 삭제됐거나 미등록): {}", scheduleName);
                return;
            } catch (Exception e) {
                log.warn("[EventBridge] 스케줄 삭제 실패 {}/{}회: {}", attempt, maxAttempts, scheduleName, e);
                if (attempt == maxAttempts) {
                    log.error("[EventBridge] 스케줄 삭제 최종 실패: {}", scheduleName, e);
                }
            }
        }
    }


    // KST -> UTC 변환(우리나라시간-9)
    /**
     * at(yyyy-MM-ddTHH:mm:ss) 형식
     * at(2026-04-16T08:43:30) -> 2026년 4월 16일 08시 43분 30초에 1회 실행
     */
    private String toUtcString(LocalDateTime kstTime) {
        return kstTime.atZone(ZoneId.of("Asia/Seoul"))
                .withZoneSameInstant(ZoneId.of("UTC"))
                .format(TARGET_TIME_FMT);
    }

    private String toAt(LocalDateTime dateTime) {
        return "at(" + toUtcString(dateTime) + ")";
    }
}