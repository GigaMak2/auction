package com.example.auction.domain.auction.eventBridge.dto;

import com.example.auction.domain.auction.eventBridge.entity.AuctionScheduleOutbox;

import java.time.LocalDateTime;

public record OutboxAdminResponse(
        Long id,
        Long auctionId,
        String eventType,
        LocalDateTime scheduledAt,
        String status,
        int attemptCount,
        LocalDateTime lastAttemptedAt,
        LocalDateTime createdAt
) {
    public static OutboxAdminResponse from(AuctionScheduleOutbox outbox) {
        return new OutboxAdminResponse(
                outbox.getId(),
                outbox.getAuctionId(),
                outbox.getEventType(),
                outbox.getScheduledAt(),
                outbox.getStatus(),
                outbox.getAttemptCount(),
                outbox.getLastAttemptedAt(),
                outbox.getCreatedAt()
        );
    }
}
