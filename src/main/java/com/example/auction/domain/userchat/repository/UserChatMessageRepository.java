package com.example.auction.domain.userchat.repository;

import com.example.auction.domain.userchat.entity.UserChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserChatMessageRepository extends JpaRepository<UserChatMessage, Long>, UserChatMessageCustomRepository {
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserChatMessage m WHERE m.createdAt < :dateTime")
    int deleteAllByCreatedAtBefore(@Param("dateTime") LocalDateTime dateTime);
}