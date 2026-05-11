package com.example.auction.domain.userchat.entity;

import com.example.auction.common.entity.CreatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChatMessage extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static UserChatMessage of(Long roomId, Long senderId, String content) {
        UserChatMessage userChatMessage = new UserChatMessage();
        userChatMessage.roomId = roomId;
        userChatMessage.senderId = senderId;
        userChatMessage.content = content;
        return userChatMessage;
    }
}