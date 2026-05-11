package com.example.auction.domain.auth.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.auth.dto.request.AuthAdminSignupRequest;
import com.example.auction.domain.auth.dto.response.AuthSignupResponse;
import com.example.auction.domain.auth.service.AuthAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AuthAdminController {

    private final AuthAdminService authAdminService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<AuthSignupResponse>> signup(@Valid @RequestBody AuthAdminSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "관리자 회원가입했습니다", authAdminService.signup(request)));
    }
}
