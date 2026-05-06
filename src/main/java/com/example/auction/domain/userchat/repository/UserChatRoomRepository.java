package com.example.auction.domain.userchat.repository;

import com.example.auction.domain.userchat.entity.UserChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, Long> {
}