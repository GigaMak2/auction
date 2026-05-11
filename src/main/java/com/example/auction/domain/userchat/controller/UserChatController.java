package com.example.auction.domain.userchat.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.userchat.dto.UserChatMessageListResponse;
import com.example.auction.domain.userchat.dto.UserChatRoomResponse;
import com.example.auction.domain.userchat.service.UserChatService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-chat")
public class UserChatController {

    private final UserChatService userChatService;

    @GetMapping("/rooms")
    public ResponseEntity<BaseResponse<List<UserChatRoomResponse>>> getRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "채팅방 목록을 조회했습니다", userChatService.getRooms(userId)));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<BaseResponse<UserChatMessageListResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "메시지 목록을 조회했습니다", userChatService.getMessages(roomId, userId, cursor, size)));
    }
}
