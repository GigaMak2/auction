package com.example.auction.domain.category.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CategoryErrorEnum implements ErrorEnumInterface {

    // 카테고리 관련
    DUPLICATED_CATEGORY(HttpStatus.CONFLICT, "이미 존재하는 카테고리입니다");

    private final HttpStatus status;
    private final String message;

    CategoryErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}