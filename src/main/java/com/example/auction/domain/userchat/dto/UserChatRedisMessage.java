package com.example.auction.domain.userchat.dto;

import java.time.LocalDateTime;

public record UserChatRedisMessage(
        Long id,
        Long roomId,
        Long senderId,
        String content,
        LocalDateTime createdAt
) {}
