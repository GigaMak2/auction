package com.example.auction.domain.userchat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserChatMessageRequest(
        @NotBlank
        String content
) {}
