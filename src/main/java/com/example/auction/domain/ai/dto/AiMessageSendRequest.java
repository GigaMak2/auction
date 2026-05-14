package com.example.auction.domain.ai.dto;

public record AiMessageSendRequest(
        // @Valid 제거 - 검증은 AiService.streamMessage() 내부 Flux.defer()에서 수행 (SSE MediaType 충돌 방지)
        String content
) {}