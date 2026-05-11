package com.example.auction.domain.auction.eventbridge;

import java.time.LocalDateTime;

public record AuctionCreatedEventBridge(
        Long auctionId,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {}
