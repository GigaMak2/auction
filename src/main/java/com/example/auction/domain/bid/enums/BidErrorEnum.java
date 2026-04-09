package com.example.auction.domain.bid.enums;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
 public enum BidErrorEnum implements ErrorEnumInterface {

     // 입찰 관련 에러
     INVALID_BID(HttpStatus.BAD_REQUEST, "입찰을 찾을 수 없습니다"),
    AUCTION_RESULT_NOT_FOUND(HttpStatus.BAD_REQUEST, "경매 결과를 찾을 수 없습니다"),
    BID_PRICE_NOT_LOWER(HttpStatus.BAD_REQUEST, "최대 가격보다 적은 금액을 입찰해주세요");

     private final HttpStatus status;
     private final String message;

     BidErrorEnum(HttpStatus status, String message) {
         this.status = status;
         this.message = message;
     }
 }