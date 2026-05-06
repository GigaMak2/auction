package com.example.auction.domain.userchat.repository;

import com.example.auction.domain.userchat.entity.UserChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChatMessageRepository extends JpaRepository<UserChatMessage, Long>, UserChatMessageCustomRepository {
}