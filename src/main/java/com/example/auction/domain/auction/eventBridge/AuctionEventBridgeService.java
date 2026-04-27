package com.example.auction.domain.auction.eventBridge;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEventBridgeService {

    private final SchedulerClient schedulerClient;
    private final ObjectMapper objectMapper;

    // 이벤트브릿지가 어떤 람다 함수를 실행할지 찾을 때 쓰는 경로. 계정번호와 실제 람다함수이름이 필요함
    @Value("${aws.eventbridge.lambda-arn}")
    private String lambdaArn;
    // 이벤트브릿지가 람다함수 실행권한이 있음을 증명할 때 사용
    @Value("${aws.eventbridge.role-arn}")
    private String roleArn;

    private static final DateTimeFormatter TARGET_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 트랜잭션 커밋 이후 eventBridge 스케줄 등록하여 고아 스케줄 방지(db에는 없고 aws 스케줄에만 있는 경우 방지)
    // 3회 재시도
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionCreated(AuctionCreatedEventBridge event) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // 경매 시작/종료 5분전으로 세팅
                registerStartSchedule(event.auctionId(), event.startedAt());
                registerEndSchedule(event.auctionId(), event.endedAt());
                return; // 성공하면 종료
            } catch (Exception e) {
                log.warn("[EventBridge] 스케줄 등록 실패 {}/{}회 - auctionId={}",
                        attempt, maxAttempts, event.auctionId(), e);
                if (attempt == maxAttempts) {
                    log.error("[EventBridge] 스케줄 등록 최종 실패 - auctionId={}, 수동 확인 필요",
                            event.auctionId(), e);
                }
            }
        }
    }

    // 경매 시작 스케줄 등록(5분 전)
    public void registerStartSchedule(Long auctionId, LocalDateTime startedAt) {
        if (isPast(startedAt.minusMinutes(5))) {
            log.warn("[EventBridge] 현재보다 과거로 시작 시간 등록: auctionId={}", auctionId);
            return;
        }
        registerSchedule(auctionId, startedAt.minusMinutes(5), startedAt, "START");
    }

    // 경매 종료 스케줄 등록(5분 전)
    public void registerEndSchedule(Long auctionId, LocalDateTime endedAt) {
        if (isPast(endedAt.minusMinutes(5))) {
            log.warn("[EventBridge] 현재보다 과거로 종료 시간 등록: auctionId={}", auctionId);
            return;
        }
        registerSchedule(auctionId, endedAt.minusMinutes(5), endedAt, "END");
    }

    // KST 기준 시간이 현재 UTC보다 과거인지 확인
    private boolean isPast(LocalDateTime kstTime) {
        LocalDateTime utc = kstTime.atZone(ZoneId.of("Asia/Seoul"))
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime();
        return utc.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private void registerSchedule(
            Long auctionId, LocalDateTime dateTime, LocalDateTime targetTime, String action
    ) {
        String scheduleName = "auction-" + action.toLowerCase() + "-" + auctionId;
        String atExpression = toAt(dateTime);
        String input;
        try {
            input = objectMapper.writeValueAsString(Map.of(
                    "auctionId", auctionId,
                    "action", action,
                    "targetTime", targetTime.format(TARGET_TIME_FMT)
            ));
        } catch (Exception e) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_SCHEDULE_SERIALIZATION_FAILED);
        }

        try {
            schedulerClient.createSchedule(r -> r
                    .name(scheduleName)
                    .scheduleExpression(atExpression)
                    .scheduleExpressionTimezone("UTC")
                    .flexibleTimeWindow(w -> w.mode(FlexibleTimeWindowMode.OFF))
                    .target(t -> t
                            .arn(lambdaArn)
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
                            .arn(lambdaArn)
                            .roleArn(roleArn)
                            .input(input))
                    .actionAfterCompletion(ActionAfterCompletion.DELETE) // 실행 후 자동 삭제(경매 시작/종료는 1번씩이니까)
            );
        }
    }

    // KST -> UTC 변환(우리나라시간-9)
    /**
     * at(yyyy-MM-ddTHH:mm:ss) 형식
     * at(2026-04-16T08:43:30) -> 2026년 4월 16일 08시 43분 30초에 1회 실행
     */
    private String toAt(LocalDateTime dateTime) {
        ZonedDateTime kst = dateTime.atZone(ZoneId.of("Asia/Seoul"));
        ZonedDateTime utc = kst.withZoneSameInstant(ZoneId.of("UTC"));

        return String.format("at(%d-%02d-%02dT%02d:%02d:%02d)",
                utc.getYear(),
                utc.getMonthValue(),
                utc.getDayOfMonth(),
                utc.getHour(),
                utc.getMinute(),
                utc.getSecond());
    }
}