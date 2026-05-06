package com.example.auction.domain.userchat.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.domain.userchat.dto.UserChatMessageRequest;
import com.example.auction.domain.userchat.service.UserChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserChatWebSocketController {

    private final UserChatService userChatService;

    @MessageMapping("/chat/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload UserChatMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        userChatService.sendMessage(roomId, userId, request);
    }
}
