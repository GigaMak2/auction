package com.example.auction.domain.user.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.user.dto.UserDetailGetResponse;
import com.example.auction.domain.user.dto.UserListGetResponse;
import com.example.auction.domain.user.dto.UserSearchCondition;
import com.example.auction.domain.user.dto.UserWithdrawResponse;
import com.example.auction.domain.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<UserListGetResponse>>> getUserList(
            @Valid @ModelAttribute UserSearchCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "유저 목록 조회 요청 성공", userAdminService.getUserList(condition)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserDetailGetResponse>> getUserDetail(
            @PathVariable Long userId
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "유저 상세 조회 요청 성공", userAdminService.getUserDetail(userId)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserWithdrawResponse>> forceWithdraw(
            @PathVariable Long userId
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "유저 강제 탈퇴 요청 성공", userAdminService.forceWithdraw(userId)));
    }
}
