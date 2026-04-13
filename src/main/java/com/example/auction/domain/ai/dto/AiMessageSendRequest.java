package com.example.auction.domain.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiMessageSendRequest(
        @NotBlank(message = "메시지 내용을 입력해 주세요")
        // 코드래빗은 2000자를 제안했지만, 현재 서비스는 짧은 질문에 대답하는 채팅봇이고 긴 문서를 분석하는 용도가 아니기에 500자 정도면 충분할것같음
        @Size(max = 500, message = "메시지는 500자 이하여야 합니다") // AI 처리 비용 및 서버 부하 방지
        String content
) {
}