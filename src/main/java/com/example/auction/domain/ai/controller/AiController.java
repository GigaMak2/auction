package com.example.auction.domain.ai.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.domain.ai.dto.AiMessageSendRequest;
import com.example.auction.domain.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // AI 메시지 전송 (SSE 스트리밍)
    @PostMapping(
            value = "/rooms/{roomId}/messages",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> sendMessage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestBody AiMessageSendRequest request
    ) {
        log.info("[AiController] sendMessage — userId={}, roomId={}", userDetails.getUserId(), roomId); // AI 채팅 요청 유입 추적 — 비정상 요청 빈도 및 유저별 사용량 모니터링용
        return aiService.streamMessage(roomId, userDetails.getUserId(), request.content());
    }
}