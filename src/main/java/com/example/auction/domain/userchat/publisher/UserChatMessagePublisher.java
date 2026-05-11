package com.example.auction.domain.userchat.publisher;

import com.example.auction.domain.userchat.dto.UserChatRedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserChatMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(Long roomId, UserChatRedisMessage message) {
        try {
            String topic = "user-chat-room:" + roomId;
            redisTemplate.convertAndSend(topic, message);
        } catch (RuntimeException e) {
            log.error("[UserChat] 메시지 발행 실패: roomId={}", roomId, e);
        }
    }
}