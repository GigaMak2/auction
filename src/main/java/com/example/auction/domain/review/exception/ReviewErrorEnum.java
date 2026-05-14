package com.example.auction.domain.review.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ReviewErrorEnum implements ErrorEnumInterface {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다"),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 해당 경매에 리뷰를 작성했습니다"),
    REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "리뷰 작성자가 아닙니다"),
    REVIEW_MODIFY_NO_CONTENT(HttpStatus.BAD_REQUEST, "수정할 내용이 없습니다"),
    REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "경매 구매자 또는 판매자만 리뷰를 작성할 수 있습니다"),
    REVIEW_IMAGE_INVALID_TYPE(HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 형식입니다 (jpeg, png, webp만 가능)"),
    REVIEW_IMAGE_INVALID_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 URL입니다");

    private final HttpStatus status;
    private final String message;

    ReviewErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}