package com.example.auction.domain.ai.controller;

import com.example.auction.domain.ai.dto.AiMessageSendRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiController {

    // AI 메시지 전송 (SSE 스트리밍)
    @PostMapping("/rooms/{roomId}/messages")
    public void sendMessage(
            @PathVariable Long roomId,
            @RequestBody @Valid AiMessageSendRequest request
    ) {
        // TODO: SSE 스트리밍 구현 예정
    }
}