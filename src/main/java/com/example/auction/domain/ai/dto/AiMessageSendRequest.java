package com.example.auction.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiMessageSendRequest(
        @NotBlank(message = "메시지 내용을 입력해 주세요")
        String content
) {
}