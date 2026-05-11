package com.example.auction.domain.userchat.dto;

import com.example.auction.domain.userchat.entity.UserChatRoom;

import java.time.LocalDateTime;

public record UserChatRoomResponse(
        Long id,
        Long auctionId,
        Long counterpartId,
        LocalDateTime createdAt
) {
    public static UserChatRoomResponse of(UserChatRoom userChatRoom, Long userId) {
        Long counterpartId = userChatRoom.getBuyerId().equals(userId) ? userChatRoom.getSellerId() : userChatRoom.getBuyerId();
        return new UserChatRoomResponse(
                userChatRoom.getId(),
                userChatRoom.getAuctionId(),
                counterpartId,
                userChatRoom.getCreatedAt()
        );
    }
}
