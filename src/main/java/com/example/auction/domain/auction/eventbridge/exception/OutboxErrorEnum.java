package com.example.auction.domain.auction.eventbridge.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OutboxErrorEnum implements ErrorEnumInterface {

    OUTBOX_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "해당하는 이벤트를 찾을 수 없습니다"
    )

    ;

    private final HttpStatus status;
    private final String message;

    OutboxErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
