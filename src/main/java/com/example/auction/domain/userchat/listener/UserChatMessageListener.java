package com.example.auction.domain.userchat.listener;

import com.example.auction.domain.userchat.dto.UserChatRedisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserChatMessageListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            UserChatRedisMessage redisMessage = objectMapper.readValue(
                    message.getBody(), UserChatRedisMessage.class);
            messagingTemplate.convertAndSend("/sub/chat/" + redisMessage.roomId(), redisMessage);
        } catch (Exception e) {
            log.error("[UserChat] 메시지 브로드캐스트 실패", e);
        }
    }
}
