package com.example.auction.domain.userchat.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.userchat.dto.*;
import com.example.auction.domain.userchat.entity.UserChatMessage;
import com.example.auction.domain.userchat.entity.UserChatRoom;
import com.example.auction.domain.userchat.exception.UserChatErrorEnum;
import com.example.auction.domain.userchat.publisher.UserChatMessagePublisher;
import com.example.auction.domain.userchat.repository.UserChatMessageRepository;
import com.example.auction.domain.userchat.repository.UserChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserChatService {

    private final UserRepository userRepository;
    private final UserChatRoomRepository userChatRoomRepository;
    private final UserChatMessageRepository userChatMessageRepository;
    private final UserChatMessagePublisher userChatMessagePublisher;

    @Transactional
    public void createRoom(Long buyerId, Long sellerId, Long auctionId) {
        userRepository.findByIdAndDeletedFalse(buyerId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        userRepository.findByIdAndDeletedFalse(sellerId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        if (userChatRoomRepository.existsByBuyerIdAndSellerIdAndAuctionId(buyerId, sellerId, auctionId)) {
            return;
        }

        UserChatRoom userChatRoom = UserChatRoom.of(buyerId, sellerId, auctionId);
        userChatRoomRepository.save(userChatRoom);
    }

    @Transactional(readOnly = true)
    public List<UserChatRoomResponse> getRooms(Long userId) {
        return userChatRoomRepository.findAllByBuyerIdOrSellerIdOrderByCreatedAtDesc(userId, userId)
                .stream()
                .map(room -> UserChatRoomResponse.of(room, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserChatMessageListResponse getMessages(Long roomId, Long userId, Long cursor, int size) {
        validateParticipant(roomId, userId);
        List<UserChatMessageResponse> messages = userChatMessageRepository.findByCursor(roomId, cursor, size)
                .stream()
                .map(UserChatMessageResponse::from)
                .toList();
        return UserChatMessageListResponse.of(messages, size);
    }

    @Transactional
    public void sendMessage(Long roomId, Long userId, UserChatMessageRequest request) {
        validateParticipant(roomId, userId);
        UserChatMessage message = UserChatMessage.of(roomId, userId, request.content());
        userChatMessageRepository.save(message);

        UserChatRedisMessage redisMessage = new UserChatRedisMessage(
                message.getId(),
                roomId,
                userId,
                request.content(),
                message.getCreatedAt()
        );
        userChatMessagePublisher.publish(roomId, redisMessage);
    }

    private void validateParticipant(Long roomId, Long userId) {
        UserChatRoom room = userChatRoomRepository.findById(roomId).orElseThrow(
                () -> new ServiceErrorException(UserChatErrorEnum.USER_CHAT_ROOM_NOT_FOUND));
        if (!room.getBuyerId().equals(userId) && !room.getSellerId().equals(userId)) {
            throw new ServiceErrorException(UserChatErrorEnum.USER_CHAT_ROOM_FORBIDDEN);
        }
    }
}
