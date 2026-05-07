package com.example.auction.domain.notification.publisher;

import com.example.auction.domain.notification.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHANNEL = "auction:notification";

    public void publish(NotificationMessage message) {
        try {
            redisTemplate.convertAndSend(CHANNEL, message);
        } catch (RuntimeException e) {
            log.error("[NotificationMessagePublisher] 알림 메시지 발행 실패 — channel={}, type={}, receiverId={}",
                    CHANNEL, message.type(), message.receiverId(), e);
        }
    }
}