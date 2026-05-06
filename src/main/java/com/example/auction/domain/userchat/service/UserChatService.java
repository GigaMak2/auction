package com.example.auction.domain.userchat.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.userchat.entity.UserChatRoom;
import com.example.auction.domain.userchat.repository.UserChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserChatService {

    private final UserRepository userRepository;
    private final UserChatRoomRepository userChatRoomRepository;

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
}
