package com.example.auction.domain.user.exception;

import com.example.auction.common.exception.ErrorEnumInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
 public enum UserErrorEnum implements ErrorEnumInterface {

     // 유저 관련
     INVALID_MINIMUM_USER(HttpStatus.BAD_REQUEST, "회원을 찾을 수 없습니다");

     private final HttpStatus status;
     private final String message;

     UserErrorEnum(HttpStatus status, String message) {
         this.status = status;
         this.message = message;
     }
 }