package com.example.auction.domain.auction.search.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuctionSearchErrorEnum implements ErrorEnumInterface {

    AUCTION_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "경매 Document를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;

    AuctionSearchErrorEnum(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
