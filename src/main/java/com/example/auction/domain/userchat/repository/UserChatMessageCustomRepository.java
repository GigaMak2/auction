package com.example.auction.domain.userchat.repository;

import com.example.auction.domain.userchat.entity.UserChatMessage;

import java.util.List;

public interface UserChatMessageCustomRepository {
    List<UserChatMessage> findByCursor(Long roomId, Long cursor, int size);
}
