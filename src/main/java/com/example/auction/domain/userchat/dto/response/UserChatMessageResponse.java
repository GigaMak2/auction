package com.example.auction.domain.userchat.dto.response;

import com.example.auction.domain.userchat.entity.UserChatMessage;

import java.time.LocalDateTime;

public record UserChatMessageResponse(
        Long id,
        Long senderId,
        String content,
        LocalDateTime createdAt
) {
    public static UserChatMessageResponse from(UserChatMessage userChatMessage) {
        return new UserChatMessageResponse(
                userChatMessage.getId(),
                userChatMessage.getSenderId(),
                userChatMessage.getContent(),
                userChatMessage.getCreatedAt()
        );
    }
}