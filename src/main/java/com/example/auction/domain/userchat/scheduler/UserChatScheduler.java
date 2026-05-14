package com.example.auction.domain.userchat.scheduler;

import com.example.auction.domain.userchat.repository.UserChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserChatScheduler {

    private final UserChatMessageRepository userChatMessageRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "userChatScheduler", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void deleteOldMessages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        int deleted = userChatMessageRepository.deleteAllByCreatedAtBefore(threshold);
        log.info("[UserChatScheduler] 30일 이전 메시지 삭제 완료 | 기준시각: {}, 삭제건수: {}건", threshold, deleted);
    }
}