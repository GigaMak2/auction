
package com.example.auction.domain.auction.eventBridge.entity;

import java.time.LocalDateTime;

public record AuctionCreatedEventBridge(
        Long auctionId,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {}
