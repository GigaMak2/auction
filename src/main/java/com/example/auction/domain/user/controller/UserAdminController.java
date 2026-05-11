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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
                HttpStatus.OK.name(), "사용자 목록을 조회했습니다", userAdminService.getUserList(condition)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserDetailGetResponse>> getUserDetail(
            @PathVariable Long userId
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "사용자를 조회했습니다", userAdminService.getUserDetail(userId)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserWithdrawResponse>> forceWithdraw(
            @PathVariable Long userId
    ) {
        log.info("[UserAdminController] forceWithdraw — userId={}", userId); // 어드민 유저 강제 탈퇴 추적용
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "사용자를 강제 탈퇴했습니다", userAdminService.forceWithdraw(userId)));
    }
}
