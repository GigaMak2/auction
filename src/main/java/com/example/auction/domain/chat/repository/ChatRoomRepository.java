package com.example.auction.domain.chat.repository;

import com.example.auction.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}