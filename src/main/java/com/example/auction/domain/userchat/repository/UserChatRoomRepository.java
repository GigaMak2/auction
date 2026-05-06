package com.example.auction.domain.userchat.repository;

import com.example.auction.domain.userchat.entity.UserChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, Long> {
    boolean existsByBuyerIdAndSellerIdAndAuctionId(Long buyerId, Long sellerId, Long auctionId);

    List<UserChatRoom> findAllByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);
}