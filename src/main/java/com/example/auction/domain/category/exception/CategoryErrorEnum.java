package com.example.auction.domain.category.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CategoryErrorEnum implements ErrorEnumInterface {

    // 카테고리 관련
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다"),
    DUPLICATED_CATEGORY(HttpStatus.CONFLICT, "이미 존재하는 카테고리입니다"),
    CATEGORY_MAX_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "더 이상 하위 카테고리를 추가할 수 없습니다"),
    CATEGORY_CANNOT_BE_OWN_PARENT(HttpStatus.BAD_REQUEST, "자기 자신을 상위 카테고리로 지정할 수 없습니다"),
    CATEGORY_CIRCULAR_REFERENCE(HttpStatus.BAD_REQUEST, "순환 참조가 발생하여 카테고리를 이동할 수 없습니다");

    private final HttpStatus status;
    private final String message;

    CategoryErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}