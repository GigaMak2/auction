package com.example.auction.domain.user.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.user.dto.UserLoginRequest;
import com.example.auction.domain.user.dto.UserLoginResponse;
import com.example.auction.domain.user.dto.UserSignupRequest;
import com.example.auction.domain.user.dto.UserSignupResponse;
import com.example.auction.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<UserSignupResponse>> signup(@Valid @RequestBody UserSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "회원가입 요청 성공", userService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<UserLoginResponse>> login(@Valid @RequestBody UserLoginRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "로그인 요청 성공", userService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<UserLoginResponse>> refreshToken(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "토큰 재발급 요청 성공", userService.refreshToken(refreshToken)));
    }
}
