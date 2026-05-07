package com.example.auction.domain.auth.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.auth.dto.AuthAdminSignupRequest;
import com.example.auction.domain.auth.dto.AuthSignupResponse;
import com.example.auction.domain.auth.service.AuthAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AuthAdminController {

    private final AuthAdminService authAdminService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<AuthSignupResponse>> signup(@Valid @RequestBody AuthAdminSignupRequest request) {
        log.info("[AuthAdminController] signup — email={}", request.email()); // 어드민 계정 생성 추적용
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "관리자 회원가입 요청 성공", authAdminService.signup(request)));
    }
}
