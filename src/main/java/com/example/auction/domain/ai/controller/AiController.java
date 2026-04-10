package com.example.auction.domain.ai.controller;

import com.example.auction.domain.ai.dto.AiMessageSendRequest;
import com.example.auction.domain.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
            @PathVariable Long roomId,
            @RequestBody @Valid AiMessageSendRequest request
    ) {
        // TODO: userId from security context
        return aiService.streamMessage(roomId, 0L, request.content());
    }
}