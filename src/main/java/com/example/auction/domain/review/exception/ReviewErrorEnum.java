package com.example.auction.domain.review.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReviewErrorEnum implements ErrorEnumInterface {

    // 리뷰 관련
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;

    ReviewErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}