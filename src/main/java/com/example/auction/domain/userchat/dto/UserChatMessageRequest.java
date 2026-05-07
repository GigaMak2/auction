package com.example.auction.domain.userchat.dto;

import jakarta.validation.constraints.NotBlank;

public record UserChatMessageRequest(
        @NotBlank
        String content
) {}
