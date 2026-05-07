package com.example.auction.domain.userchat.repository;

import com.example.auction.domain.userchat.entity.UserChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, Long> {
    boolean existsByBuyerIdAndSellerIdAndAuctionId(Long buyerId, Long sellerId, Long auctionId);

    List<UserChatRoom> findAllByBuyerIdOrSellerIdOrderByCreatedAtDesc(Long buyerId, Long sellerId);

    @Query("SELECT COUNT(r) > 0 FROM UserChatRoom r WHERE r.id = :roomId AND (r.buyerId = :userId OR r.sellerId = :userId)")
    boolean existsByIdAndMember(@Param("roomId") Long roomId, @Param("userId") Long userId);
}