package com.example.auction.domain.ai.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AiErrorEnum implements ErrorEnumInterface {

    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스가 일시적으로 중단되었습니다"),
    INVALID_MESSAGE_CONTENT(HttpStatus.BAD_REQUEST, "메시지 내용을 입력해 주세요"),
    MESSAGE_TOO_LONG(HttpStatus.BAD_REQUEST, "메시지는 500자 이하여야 합니다"),
    INVALID_TOOL_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다"),
    TOOL_NO_DATA(HttpStatus.NOT_FOUND, "조회된 데이터가 없습니다");

    private final HttpStatus status;
    private final String message;

    AiErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}