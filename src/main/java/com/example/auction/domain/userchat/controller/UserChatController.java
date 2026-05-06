package com.example.auction.domain.userchat.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.userchat.dto.UserChatMessageListResponse;
import com.example.auction.domain.userchat.dto.UserChatRoomResponse;
import com.example.auction.domain.userchat.service.UserChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                HttpStatus.OK.name(), "채팅방 목록 조회 성공", userChatService.getRooms(userId)));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<BaseResponse<UserChatMessageListResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "메시지 목록 조회 성공", userChatService.getMessages(roomId, userId, cursor, size)));
    }
}
