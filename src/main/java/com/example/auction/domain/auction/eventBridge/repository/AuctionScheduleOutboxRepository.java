package com.example.auction.domain.auction.eventBridge.repository;

import com.example.auction.domain.auction.eventBridge.entity.AuctionScheduleOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AuctionScheduleOutboxRepository extends JpaRepository<AuctionScheduleOutbox, Long> {

    List<AuctionScheduleOutbox> findByStatusInAndAttemptCountLessThan(List<String> statuses, int maxAttempts);

    List<AuctionScheduleOutbox> findByStatus(String status);

    @Modifying
    @Transactional
    @Query("UPDATE AuctionScheduleOutbox o SET o.status = 'PUBLISHED' WHERE o.auctionId = :auctionId AND o.eventType = :eventType AND o.status IN ('PENDING', 'FAILED')")
    void markPublished(@Param("auctionId") Long auctionId, @Param("eventType") String eventType);
}
