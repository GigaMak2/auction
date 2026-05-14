package com.example.auction.domain.userchat.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserChatErrorEnum implements ErrorEnumInterface {

    USER_CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"),
    USER_CHAT_ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 채팅방만 이용할 수 있습니다");

    private final HttpStatus status;
    private final String message;

    UserChatErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}