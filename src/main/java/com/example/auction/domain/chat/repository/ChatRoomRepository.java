package com.example.auction.domain.chat.repository;

import com.example.auction.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 내 채팅방 목록 최신순 조회
    List<ChatRoom> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 채팅방 조회 + 본인 소유 검증
    Optional<ChatRoom> findByIdAndUserId(Long id, Long userId);
}