package com.example.auction.domain.userchat.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.domain.userchat.dto.UserChatMessageRequest;
import com.example.auction.domain.userchat.service.UserChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserChatWebSocketController {

    private final UserChatService userChatService;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload UserChatMessageRequest request,
            Principal principal
    ) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken token)) {
            log.warn("[UserChat] 인증되지 않은 메시지 전송 시도 roomId={}", roomId);
            return;
        }
        CustomUserDetails userDetails = (CustomUserDetails) token.getPrincipal();
        Long userId = userDetails.getUserId();
        userChatService.sendMessage(roomId, userId, request);
    }
}
