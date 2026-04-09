package com.example.auction.domain.chat.repository;

import com.example.auction.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom {

    // 채팅방 삭제 시 메시지 cascade 하드딜리트
    void deleteAllByRoomId(Long roomId);

    // 스케줄러 - 30일 이전 메시지 자동 삭제
    void deleteAllByCreatedAtBefore(LocalDateTime dateTime);
}