package com.example.auction.domain.chat.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.chat.dto.ChatMessageListResponse;
import com.example.auction.domain.chat.dto.ChatRoomResponse;
import com.example.auction.domain.chat.dto.ChatRoomUpdateRequest;
import com.example.auction.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 채팅방 생성
    @PostMapping("/rooms")
    public ResponseEntity<BaseResponse<ChatRoomResponse>> createRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[ChatController] createRoom — userId={}", userDetails.getUserId()); // 채팅방 생성 이벤트 추적 — 유저별 채팅방 생성 빈도 모니터링용
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(
                        HttpStatus.CREATED.name(),
                        "채팅방을 생성했습니다",
                        chatService.createRoom(userDetails.getUserId())
                ));
    }

    // 내 채팅방 목록 조회
    @GetMapping("/rooms")
    public ResponseEntity<BaseResponse<List<ChatRoomResponse>>> getRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("[ChatController] getRooms — userId={}", userDetails.getUserId()); // 채팅방 목록 조회 추적 — 유저별 사용 빈도 모니터링용
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "채팅방 목록을 조회했습니다",
                        chatService.getRooms(userDetails.getUserId())
                ));
    }

    // 채팅방 제목 수정
    @PatchMapping("/rooms/{roomId}")
    public ResponseEntity<BaseResponse<ChatRoomResponse>> updateRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestBody @Valid ChatRoomUpdateRequest request
    ) {
        log.info("[ChatController] updateRoom — userId={}, roomId={}", userDetails.getUserId(), roomId); // 채팅방 제목 수정 추적 — 비정상 요청 감지용
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "채팅방 제목을 수정했습니다",
                        chatService.updateTitle(roomId, userDetails.getUserId(), request.title())
                ));
    }

    // 채팅방 삭제
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<BaseResponse<Void>> deleteRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId
    ) {
        log.info("[ChatController] deleteRoom — userId={}, roomId={}", userDetails.getUserId(), roomId); // 채팅방 삭제 이벤트 추적 — 대화 이력 소멸 감지용
        chatService.deleteRoom(roomId, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "채팅방을 삭제했습니다",
                        null
                ));
    }

    // 메시지 목록 조회 (커서 기반 페이징)
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<BaseResponse<ChatMessageListResponse>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[ChatController] getMessages — userId={}, roomId={}", userDetails.getUserId(), roomId); // 메시지 목록 조회 추적 — 유저별 대화 접근 빈도 모니터링용
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        HttpStatus.OK.name(),
                        "메시지 목록을 조회했습니다",
                        chatService.getMessages(roomId, userDetails.getUserId(), cursor, size)
                ));
    }
}