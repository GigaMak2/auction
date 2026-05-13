package com.example.auction.domain.auction.eventbridge.entity;

import java.time.LocalDateTime;

public record AuctionCreatedEventBridge(
        Long auctionId,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {}
