package com.example.auction.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion;
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEventBridgeService {

    private final SchedulerClient schedulerClient;

    // 이벤트브릿지가 어떤 람다 함수를 실행할지 찾을 때 쓰는 경로. 계정번호와 실제 람다함수이름이 필요함
    @Value("${LAMBDA_ARN}")
    private String lambdaArn;
    // 이벤트브릿지가 람다함수 실행권한이 있음을 증명할 때 사용
    @Value("${ROLE_ARN}")
    private String roleArn;

    // 경매 시작 스케줄 등록
    public void registerStartSchedule(Long auctionId, LocalDateTime startedAt) {
        if (isPast(startedAt)) {
            log.warn("[EventBridge] 현재보다 과거로 시작 시간 등록: auctionId={}", auctionId);
            return;
        }
        registerSchedule(auctionId, startedAt, "START");
    }

    // 경매 종료 스케줄 등록
    public void registerEndSchedule(Long auctionId, LocalDateTime endedAt) {
        if (isPast(endedAt)) {
            log.warn("[EventBridge] 현재보다 과거로 종료 시간 등록: auctionId={}", auctionId);
            return;
        }
        registerSchedule(auctionId, endedAt, "END");
    }

    // KST 기준 시간이 현재 UTC보다 과거인지 확인
    private boolean isPast(LocalDateTime kstTime) {
        LocalDateTime utc = kstTime.atZone(ZoneId.of("Asia/Seoul"))
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime();
        return utc.isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private void registerSchedule(Long auctionId, LocalDateTime dateTime, String action) {
        String scheduleName = "auction-" + action.toLowerCase() + "-" + auctionId;
        String atExpression = toAt(dateTime);

        schedulerClient.createSchedule(r -> r
                .name(scheduleName)
                .scheduleExpression(atExpression)
                .scheduleExpressionTimezone("UTC")
                .flexibleTimeWindow(w -> w.mode(FlexibleTimeWindowMode.OFF))
                .target(t -> t
                        .arn(lambdaArn)
                        .roleArn(roleArn)
                        .input("{\"auctionId\": " + auctionId + ", \"action\": \"" + action + "\"}")
                )
                .actionAfterCompletion(ActionAfterCompletion.DELETE) // 실행 후 자동 삭제(경매 시작/종료는 1번씩이니까)
        );
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