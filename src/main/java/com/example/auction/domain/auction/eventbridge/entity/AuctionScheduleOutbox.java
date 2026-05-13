package com.example.auction.domain.auction.eventbridge.entity;

import com.example.auction.common.entity.CreatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "auction_schedule_outbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionScheduleOutbox extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    public static AuctionScheduleOutbox of(Long auctionId, String eventType, LocalDateTime scheduledAt) {
        AuctionScheduleOutbox outbox = new AuctionScheduleOutbox();
        outbox.auctionId = auctionId;
        outbox.eventType = eventType;
        outbox.scheduledAt = scheduledAt;
        outbox.status = "PENDING";
        outbox.attemptCount = 0;
        return outbox;
    }

    public void markPublished() {
        this.status = "PUBLISHED";
        this.lastAttemptedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.lastAttemptedAt = LocalDateTime.now();
    }

    public void incrementAttempt() {
        this.attemptCount++;
        this.lastAttemptedAt = LocalDateTime.now();
    }

    public void resetForRetry() {
        this.status = "PENDING";
        this.attemptCount = 0;
        this.lastAttemptedAt = null;
    }
}
