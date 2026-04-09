package com.example.auction.domain.auction.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuctionErrorEnum implements ErrorEnumInterface {

    // 경매 관련 에러
     INVALID_MINIMUM_AUCTION(HttpStatus.BAD_REQUEST, "경매를 찾을 수 없습니다");

     private final HttpStatus status;
     private final String message;

     AuctionErrorEnum(HttpStatus status, String message) {
         this.status = status;
         this.message = message;
     }
 }
