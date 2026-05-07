package com.example.auction.domain.userchat.entity;

import com.example.auction.common.entity.CreatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_chat_rooms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"buyer_id", "seller_id", "auction_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChatRoom extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long buyerId;

    @Column(nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private Long auctionId;

    public static UserChatRoom of(Long buyerId, Long sellerId, Long auctionId) {
        UserChatRoom userChatRoom = new UserChatRoom();
        userChatRoom.buyerId = buyerId;
        userChatRoom.sellerId = sellerId;
        userChatRoom.auctionId = auctionId;
        return userChatRoom;
    }
}