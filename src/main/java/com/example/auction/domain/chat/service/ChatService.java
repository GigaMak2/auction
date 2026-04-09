package com.example.auction.domain.chat.service;

import com.example.auction.domain.chat.dto.ChatMessageListResponse;
import com.example.auction.domain.chat.dto.ChatRoomResponse;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import com.example.auction.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 채팅방 생성
    @Transactional
    public ChatRoomResponse createRoom(Long userId) {
        // TODO: 구현 예정
        return null;
    }

    // 내 채팅방 목록 조회
    public List<ChatRoomResponse> getRooms(Long userId) {
        // TODO: 구현 예정
        return null;
    }

    // 채팅방 삭제
    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        // TODO: 구현 예정
    }

    // 메시지 목록 조회 (커서 기반 페이징)
    public ChatMessageListResponse getMessages(Long roomId, Long userId, Long cursor, int size) {
        // TODO: 구현 예정
        return null;
    }
}