package com.example.auction.domain.auction.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuctionErrorEnum implements ErrorEnumInterface {

    // 경매 관련 에러
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다"),

    AUCTION_SEARCH_FORBIDDEN_STATUS_FILTER(HttpStatus.BAD_REQUEST, "취소된 경매는 전체조회에서 볼 수 없습니다"),
    AUCTION_SEARCH_INVLID_PRICE_RANGE(
            HttpStatus.BAD_REQUEST, 
            "조회 최소 금액이 최대 금액보다 클 수는 없습니다"
    );

    private final HttpStatus status;
    private final String message;

    AuctionErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
